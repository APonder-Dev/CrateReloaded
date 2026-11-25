package com.hazebyte.crate.cratereloaded.crate.animation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.cratereloaded.crate.OpeningLedger;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Issue #540 through the real event path: the server closes the animation inventory before
 * PlayerQuitEvent, so the quit handler must settle the opening without looking at the open inventory.
 */
class AnimationQuitTest extends BukkitTest {

    @Test
    void quitDuringAnimation_settlesTheOpening() {
        AnimationMenuListener.getInstance().register(plugin);
        OpeningLedger ledger = plugin.getOpeningLedger();
        Assertions.assertNotNull(ledger, "ledger is created on enable");

        PlayerMock player = server.addPlayer();
        Reward prize = mock(Reward.class);
        when(prize.serialize()).thenReturn("item:(dirt 1)");
        OpeningLedger.Session session = ledger.begin(player, Collections.singletonList(prize));
        Assertions.assertTrue(ledger.isPending(session.getPendingClaim().getId()));

        player.closeInventory(); // what the server does first on disconnect
        player.disconnect(); // fires PlayerQuitEvent

        Assertions.assertFalse(ledger.hasSession(player.getUniqueId()), "quit must settle the opening");
        Assertions.assertTrue(session.isSettled());
        Assertions.assertFalse(ledger.complete(player.getUniqueId()), "the animation's late ending must not pay again");
    }

    @Test
    void quitAndRejoinBeforeAnimationEnds_paysOnce() {
        // #496: quit mid-animation (prize goes to claims), rejoin before the animation would have ended, and
        // the animation used to pay a second time to the now-online player.
        AnimationMenuListener.getInstance().register(plugin);
        OpeningLedger ledger = plugin.getOpeningLedger();
        PlayerMock player = server.addPlayer();
        Reward prize = mock(Reward.class);
        when(prize.serialize()).thenReturn("item:(dirt 1)");
        OpeningLedger.Session session = ledger.begin(player, Collections.singletonList(prize));

        player.closeInventory();
        player.disconnect();
        player.reconnect();
        Assertions.assertTrue(player.isOnline());

        // The animation's scheduled ending runs now, with the player back online.
        Assertions.assertFalse(ledger.complete(player.getUniqueId()), "no second payout after rejoining");
        Assertions.assertFalse(ledger.isPending(session.getPendingClaim().getId()), "the one claim is redeemable");
    }
}
