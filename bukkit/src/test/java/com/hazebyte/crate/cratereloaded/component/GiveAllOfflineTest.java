package com.hazebyte.crate.cratereloaded.component;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.claim.Claim;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.event.CrateGiveEvent;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** #445 / #524: {@code /cr give all <crate> n offline} must put the crates in each online player's claims. */
class GiveAllOfflineTest extends BukkitTest {

    private Listener cancelFirst;

    @AfterEach
    void unregister() {
        if (cancelFirst != null) {
            HandlerList.unregisterAll(cancelFirst);
            cancelFirst = null;
        }
        server.setPlayers(0);
    }

    private static Crate anyCrate() {
        List<Crate> crates = plugin.getCrateRegistrar().getCrates();
        Assumptions.assumeFalse(crates == null || crates.isEmpty(), "no crates loaded in the mock server");
        return crates.get(0);
    }

    private static int crateItems(PlayerMock player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getCrateRegistrar().getCrate(item) != null) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Test
    void giveAllOffline_sendsToEveryOnlinePlayersClaims_notInventories() {
        Crate crate = anyCrate();
        PlayerMock a = server.addPlayer();
        PlayerMock b = server.addPlayer();

        CorePlugin.getJavaPluginComponent()
                .getGiveCrateComponent()
                .giveCrateToAllOnlinePlayers(server.getConsoleSender(), crate, 2, true);

        for (PlayerMock player : new PlayerMock[] {a, b}) {
            Collection<Claim> claims =
                    plugin.getClaimManager().getClaims(player).join();
            Assertions.assertEquals(1, claims.size(), "one claim per player");
            Assertions.assertEquals(0, crateItems(player), "nothing goes straight into the inventory");
        }
    }

    @Test
    void cancellingOnePlayersGive_doesNotSkipTheOthers() {
        Crate crate = anyCrate();
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();
        cancelFirst = new Listener() {
            @EventHandler
            public void onGive(CrateGiveEvent event) {
                if (event.getTarget().getUniqueId().equals(first.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(cancelFirst, plugin);

        CorePlugin.getJavaPluginComponent()
                .getGiveCrateComponent()
                .giveCrateToAllOnlinePlayers(server.getConsoleSender(), crate, 1, false);

        Assertions.assertEquals(0, crateItems(first));
        Assertions.assertTrue(crateItems(second) > 0, "the give used to stop at the first cancelled player");
    }
}
