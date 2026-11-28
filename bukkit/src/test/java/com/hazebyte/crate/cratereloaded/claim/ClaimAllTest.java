package com.hazebyte.crate.cratereloaded.claim;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.claim.Claim;
import com.hazebyte.crate.api.event.ClaimEvent;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.crate.OpeningLedger;
import com.hazebyte.crate.cratereloaded.model.RewardImpl;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** {@code /crate claim all}: a claim is deleted only once it was paid, and is never paid twice. */
class ClaimAllTest extends BukkitTest {

    private Collection<Claim> claimsOf(PlayerMock player) {
        // The claim thread is FIFO: this also waits for every read, save and delete queued before it.
        return plugin.getClaimRegistrar().getClaims(player).join();
    }

    private void claimAll(PlayerMock player) {
        CorePlugin.getJavaPluginComponent().getClaimCrateComponent().claimAllRewards(player);
    }

    private int count(PlayerMock player, Material material) {
        return Arrays.stream(player.getInventory().getStorageContents())
                .filter(item -> item != null && item.getType() == material)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    @Test
    void claimWhosePayoutIsRefused_isKept() {
        // Another plugin refusing the payout (e.g. combat tag) is one way a claim gets refused; a full inventory is
        // the other. claim all used to delete each claim before trying to pay it, so a refused claim was lost.
        Listener refuse = new Listener() {
            @EventHandler
            public void onClaim(ClaimEvent event) {
                event.setCancelled(true);
            }
        };
        Bukkit.getPluginManager().registerEvents(refuse, plugin);
        try {
            PlayerMock player = server.addPlayer();
            Claim claim = plugin.getClaimRegistrar()
                    .addClaim(player, Collections.singletonList(new RewardImpl("item:(dirt 1)")))
                    .join();

            claimAll(player);
            claimsOf(player);
            server.getScheduler().performTicks(2);

            Assertions.assertEquals(0, count(player, Material.DIRT));
            Assertions.assertTrue(
                    claimsOf(player).stream().anyMatch(c -> c.getId().equals(claim.getId())),
                    "a refused claim must stay claimable");
        } finally {
            HandlerList.unregisterAll(refuse);
        }
    }

    @Test
    void twoQuickClaimAlls_payOnce() {
        PlayerMock player = server.addPlayer();
        plugin.getClaimRegistrar()
                .addClaim(player, Collections.singletonList(new RewardImpl("item:(dirt 1)")))
                .join();

        claimAll(player);
        claimAll(player); // both reads are queued before the first delete
        claimsOf(player);
        server.getScheduler().performTicks(2);

        Assertions.assertEquals(1, count(player, Material.DIRT), "one claim, one payout");
        Assertions.assertTrue(claimsOf(player).isEmpty());
    }

    @Test
    void pendingAnimationClaim_isNotTouched() {
        PlayerMock player = server.addPlayer();
        OpeningLedger ledger = plugin.getOpeningLedger();
        OpeningLedger.Session session =
                ledger.begin(player, Collections.singletonList(new RewardImpl("item:(dirt 1)")));

        claimAll(player);
        claimsOf(player);
        server.getScheduler().performTicks(2);

        Assertions.assertEquals(0, count(player, Material.DIRT), "a running animation's prize is not redeemable");
        Assertions.assertTrue(
                claimsOf(player).stream()
                        .anyMatch(
                                c -> c.getId().equals(session.getPendingClaim().getId())),
                "the pending claim stays on disk until the animation settles");
        ledger.abortAll();
    }
}
