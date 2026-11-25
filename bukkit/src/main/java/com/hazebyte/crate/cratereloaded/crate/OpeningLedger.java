package com.hazebyte.crate.cratereloaded.crate;

import com.hazebyte.crate.api.claim.Claim;
import com.hazebyte.crate.api.crate.reward.Reward;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Makes sure an animated crate opening pays out exactly once, even if the player leaves, the plugin is
 * disabled or the server crashes mid-animation (issue #540).
 *
 * <p>When an animation starts, its prize is written to the player's claim storage as a <em>pending</em>
 * claim. Then exactly one of two things happens, enforced by a compare-and-set on the session:
 *
 * <ul>
 *   <li>{@link #complete}: the animation ended normally. The pending claim is deleted and the caller
 *       delivers the reward as before.
 *   <li>{@link #abort}: the player quit or was kicked, or the plugin is disabling. If the player is
 *       online and the server doesn't use claims, the prize is delivered now and the claim is deleted.
 *       Otherwise the claim is kept and the player collects it with {@code /crate claim}, which is the
 *       only safe option for an offline player.
 * </ul>
 *
 * <p>If the server crashes, neither runs. The pending claim is already on disk, so the prize is still
 * collectable after the restart. A pending claim can't be redeemed while its animation is running
 * ({@link #isPending}), so it can't be claimed and also won.
 */
public class OpeningLedger {

    /** Persistence for pending claims; implemented by the claim manager. Operations run in order. */
    public interface PendingClaimStore {
        CompletableFuture<?> save(Claim claim);

        CompletableFuture<?> delete(Claim claim);
    }

    /** One animated opening. */
    public static final class Session {
        private final UUID playerId;
        private final Claim pendingClaim;
        private final List<Reward> prize;
        private final AtomicBoolean settled = new AtomicBoolean(false);

        Session(UUID playerId, Claim pendingClaim, List<Reward> prize) {
            this.playerId = playerId;
            this.pendingClaim = pendingClaim;
            this.prize = prize;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public Claim getPendingClaim() {
            return pendingClaim;
        }

        public List<Reward> getPrize() {
            return prize;
        }

        public boolean isSettled() {
            return settled.get();
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final PendingClaimStore store;
    private final BiFunction<OfflinePlayer, List<Reward>, Claim> claimFactory;
    private final BooleanSupplier handlingClaims;
    private final Function<UUID, Player> onlinePlayer;
    private final BiConsumer<Player, List<Reward>> deliverNow;
    private final BiConsumer<Player, Claim> notifyClaimed;
    private final Logger logger;

    /**
     * @param store where pending claims are saved and deleted
     * @param claimFactory builds the (not yet stored) pending claim for a player and prize
     * @param handlingClaims whether the server routes undeliverable rewards to claims
     * @param onlinePlayer resolves an online player by UUID, or {@code null} if offline
     * @param deliverNow gives rewards to an online player directly
     * @param notifyClaimed tells an online player their prize is waiting in {@code /crate claim}
     * @param logger for storage failures
     */
    public OpeningLedger(
            PendingClaimStore store,
            BiFunction<OfflinePlayer, List<Reward>, Claim> claimFactory,
            BooleanSupplier handlingClaims,
            Function<UUID, Player> onlinePlayer,
            BiConsumer<Player, List<Reward>> deliverNow,
            BiConsumer<Player, Claim> notifyClaimed,
            Logger logger) {
        this.store = store;
        this.claimFactory = claimFactory;
        this.handlingClaims = handlingClaims;
        this.onlinePlayer = onlinePlayer;
        this.deliverNow = deliverNow;
        this.notifyClaimed = notifyClaimed;
        this.logger = logger;
    }

    /**
     * Records the start of an animated opening and persists its prize as a pending claim. If an older
     * session for the player is still open (it should not be), it is aborted first so it can't be lost.
     *
     * @param player the player opening the crate
     * @param prize the reward(s) the player is owed if the animation can't finish
     * @return the new session
     */
    public Session begin(OfflinePlayer player, List<Reward> prize) {
        UUID playerId = player.getUniqueId();
        if (sessions.containsKey(playerId)) {
            abort(playerId);
        }
        List<Reward> copy = Collections.unmodifiableList(new ArrayList<>(prize));
        Claim claim = claimFactory.apply(player, copy);
        Session session = new Session(playerId, claim, copy);
        sessions.put(playerId, session);
        run("save", claim, () -> store.save(claim));
        return session;
    }

    /**
     * The animation ended normally. Returns {@code true} exactly once per session; the caller then delivers
     * the reward. Returns {@code false} if the session was already settled (aborted) or doesn't exist, in
     * which case the caller must not deliver anything.
     *
     * @param playerId the player
     * @return whether the caller should deliver the reward
     */
    public boolean complete(UUID playerId) {
        Session session = sessions.get(playerId);
        if (session == null || !session.settled.compareAndSet(false, true)) {
            return false;
        }
        sessions.remove(playerId, session);
        run("delete", session.pendingClaim, () -> store.delete(session.pendingClaim));
        return true;
    }

    /**
     * The animation can't finish (quit, kick, plugin disable). Settles the session if it is still open.
     *
     * @param playerId the player
     * @return {@code true} if this call settled a session
     */
    public boolean abort(UUID playerId) {
        Session session = sessions.get(playerId);
        if (session == null || !session.settled.compareAndSet(false, true)) {
            return false;
        }
        sessions.remove(playerId, session);
        Player player = onlinePlayer.apply(playerId);
        boolean online = player != null && player.isOnline();
        if (online && !handlingClaims.getAsBoolean()) {
            run("delete", session.pendingClaim, () -> store.delete(session.pendingClaim));
            deliverNow.accept(player, session.prize);
        } else if (online) {
            notifyClaimed.accept(player, session.pendingClaim); // the pending claim is now an ordinary claim
        }
        // Offline: the pending claim stays on disk; ClaimMessageListener announces it on the next join.
        return true;
    }

    /** Aborts every open session (plugin disable). */
    public void abortAll() {
        for (UUID playerId : new ArrayList<>(sessions.keySet())) {
            abort(playerId);
        }
    }

    /**
     * @param claimId a claim id
     * @return whether that claim belongs to an animation still running, so it must not be redeemed yet
     */
    public boolean isPending(UUID claimId) {
        for (Session session : sessions.values()) {
            if (!session.isSettled() && session.pendingClaim.getId().equals(claimId)) {
                return true;
            }
        }
        return false;
    }

    /** @return whether the player has an animation in progress */
    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    private void run(String action, Claim claim, java.util.function.Supplier<CompletableFuture<?>> op) {
        try {
            op.get().exceptionally(e -> {
                log(action, claim, e);
                return null;
            });
        } catch (RuntimeException e) {
            log(action, claim, e);
        }
    }

    private void log(String action, Claim claim, Throwable e) {
        logger.log(
                Level.SEVERE,
                String.format(
                        "Could not %s pending crate claim %s for %s",
                        action, claim.getId(), claim.getOwner().getUniqueId()),
                e);
    }
}
