package com.hazebyte.crate.cratereloaded.crate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hazebyte.crate.api.claim.Claim;
import com.hazebyte.crate.api.crate.reward.Reward;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Issue #540: an interrupted crate animation must pay out exactly once, and never to nobody. */
class OpeningLedgerTest {

    private final UUID playerId = UUID.randomUUID();
    private final Reward prize = mock(Reward.class);
    private final List<Reward> prizeList = Collections.singletonList(prize);

    /** Fake claim storage: claim id -> claim. */
    private final Map<UUID, Claim> stored = new ConcurrentHashMap<>();

    private final AtomicBoolean handlingClaims = new AtomicBoolean(false);
    private final AtomicInteger delivered = new AtomicInteger();
    private final AtomicInteger notified = new AtomicInteger();
    private Player online; // null = offline
    private OfflinePlayer owner;
    private OpeningLedger ledger;

    @BeforeEach
    void setUp() {
        owner = mock(OfflinePlayer.class);
        when(owner.getUniqueId()).thenReturn(playerId);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isOnline()).thenReturn(true);
        online = player;

        OpeningLedger.PendingClaimStore store = new OpeningLedger.PendingClaimStore() {
            @Override
            public CompletableFuture<?> save(Claim claim) {
                stored.put(claim.getId(), claim);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<?> delete(Claim claim) {
                stored.remove(claim.getId());
                return CompletableFuture.completedFuture(null);
            }
        };
        ledger = new OpeningLedger(
                store,
                (who, rewards) -> {
                    Claim claim = mock(Claim.class);
                    UUID id = UUID.randomUUID();
                    when(claim.getId()).thenReturn(id);
                    when(claim.getOwner()).thenReturn(who);
                    when(claim.getRewards()).thenReturn(rewards);
                    return claim;
                },
                handlingClaims::get,
                id -> online,
                (p, rewards) -> delivered.addAndGet(rewards.size()),
                (p, claim) -> notified.incrementAndGet(),
                Logger.getLogger("test"));
    }

    @Test
    void begin_persistsPrizeBeforeAnimationRuns() {
        OpeningLedger.Session session = ledger.begin(owner, prizeList);
        Assertions.assertEquals(1, stored.size());
        Assertions.assertSame(prize, stored.get(session.getPendingClaim().getId()).getRewards().get(0));
        Assertions.assertTrue(ledger.isPending(session.getPendingClaim().getId()));
    }

    @Test
    void normalCompletion_deletesClaimAndDeliversOnce() {
        ledger.begin(owner, prizeList);
        Assertions.assertTrue(ledger.complete(playerId));
        Assertions.assertFalse(ledger.complete(playerId), "second completion must not deliver again");
        Assertions.assertTrue(stored.isEmpty());
        Assertions.assertFalse(ledger.hasSession(playerId));
    }

    @Test
    void quitMidAnimation_offline_keepsClaim_andLateCompletionDeliversNothing() {
        OpeningLedger.Session session = ledger.begin(owner, prizeList);
        online = null; // the player has left

        Assertions.assertTrue(ledger.abort(playerId));
        Assertions.assertEquals(1, stored.size(), "the prize stays on disk as a claim");
        Assertions.assertFalse(ledger.isPending(session.getPendingClaim().getId()), "claim is redeemable now");
        Assertions.assertEquals(0, delivered.get());

        // The animation's scheduled ending fires later: it must not pay out a second time.
        Assertions.assertFalse(ledger.complete(playerId));
        Assertions.assertEquals(1, stored.size());
    }

    @Test
    void abortWhileOnline_withoutClaims_deliversDirectly() {
        handlingClaims.set(false);
        ledger.begin(owner, prizeList);
        Assertions.assertTrue(ledger.abort(playerId));
        Assertions.assertEquals(1, delivered.get());
        Assertions.assertTrue(stored.isEmpty());
        Assertions.assertEquals(0, notified.get());
    }

    @Test
    void abortWhileOnline_withClaims_keepsSinglePrizeAsClaim() {
        handlingClaims.set(true);
        ledger.begin(owner, prizeList);
        Assertions.assertTrue(ledger.abort(playerId));
        Assertions.assertEquals(0, delivered.get());
        Assertions.assertEquals(1, stored.size());
        Assertions.assertEquals(1, stored.values().iterator().next().getRewards().size(),
                "one key = one prize (previously the whole pre-generated list was claimed)");
        Assertions.assertEquals(1, notified.get());
    }

    @Test
    void secondBegin_settlesTheFirstSession() {
        online = null;
        ledger.begin(owner, prizeList);
        ledger.begin(owner, prizeList);
        Assertions.assertEquals(2, stored.size(), "the first prize was kept, not overwritten");
        Assertions.assertTrue(ledger.complete(playerId));
        Assertions.assertEquals(1, stored.size());
    }

    @Test
    void abortAll_settlesEveryone() {
        online = null;
        ledger.begin(owner, prizeList);
        ledger.abortAll();
        Assertions.assertFalse(ledger.hasSession(playerId));
        Assertions.assertEquals(1, stored.size());
    }

    @Test
    void completeAndAbortRacing_settleExactlyOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 500; round++) {
                stored.clear();
                delivered.set(0);
                ledger.begin(owner, prizeList);
                CountDownLatch go = new CountDownLatch(1);
                Future<Boolean> completed = pool.submit(() -> {
                    go.await();
                    return ledger.complete(playerId);
                });
                Future<Boolean> aborted = pool.submit(() -> {
                    go.await();
                    return ledger.abort(playerId);
                });
                go.countDown();
                boolean c = completed.get(5, TimeUnit.SECONDS);
                boolean a = aborted.get(5, TimeUnit.SECONDS);
                Assertions.assertTrue(c ^ a, "exactly one of complete/abort may win (round " + round + ")");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void storageFailure_isLoggedNotThrown() {
        OpeningLedger failing = new OpeningLedger(
                new OpeningLedger.PendingClaimStore() {
                    @Override
                    public CompletableFuture<?> save(Claim claim) {
                        CompletableFuture<Void> f = new CompletableFuture<>();
                        f.completeExceptionally(new java.io.IOException("disk full"));
                        return f;
                    }

                    @Override
                    public CompletableFuture<?> delete(Claim claim) {
                        throw new IllegalStateException("executor shut down");
                    }
                },
                (who, rewards) -> {
                    Claim claim = mock(Claim.class);
                    when(claim.getId()).thenReturn(UUID.randomUUID());
                    when(claim.getOwner()).thenReturn(who);
                    return claim;
                },
                () -> false,
                id -> null,
                (p, r) -> {},
                (p, c) -> {},
                Logger.getLogger("test"));
        Assertions.assertDoesNotThrow(() -> failing.begin(owner, prizeList));
        Assertions.assertDoesNotThrow(() -> failing.complete(playerId));
    }
}
