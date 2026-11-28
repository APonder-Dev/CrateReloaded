package com.hazebyte.crate.cratereloaded.claim;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.model.Config;
import com.hazebyte.crate.cratereloaded.model.RewardImpl;
import java.util.Collections;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Claim files no longer stay parsed in memory for the whole uptime once read. */
class ClaimFileUnloadTest extends BukkitTest {

    private static Config claimFile(UUID playerId) {
        return CorePlugin.getJavaPluginComponent()
                .getConfigManagerComponent()
                .getConfigWithName("claim/" + playerId + ".yml");
    }

    private static ClaimManager claims() {
        return (ClaimManager) plugin.getClaimRegistrar();
    }

    @Test
    void offlinePlayersFiles_areUnloaded_andStillReadable() {
        OfflinePlayer offline = server.getOfflinePlayer(UUID.randomUUID());
        claims().addClaim(offline, Collections.singletonList(new RewardImpl("item:(dirt 1)")))
                .join();
        Assertions.assertTrue(claimFile(offline.getUniqueId()).isLoaded());

        int unloaded = claims().unloadOfflineClaimFiles().join();

        Assertions.assertTrue(unloaded >= 1);
        Assertions.assertFalse(claimFile(offline.getUniqueId()).isLoaded(), "offline player's file is dropped");
        Assertions.assertEquals(
                1, claims().getClaims(offline).join().size(), "the claim is read back from disk when needed");
    }

    @Test
    void onlinePlayersFiles_stayLoaded_untilTheyQuit() {
        PlayerMock player = server.addPlayer();
        claims().addClaim(player, Collections.singletonList(new RewardImpl("item:(dirt 1)")))
                .join();

        claims().unloadOfflineClaimFiles().join();
        Assertions.assertTrue(claimFile(player.getUniqueId()).isLoaded(), "online players keep theirs");

        player.disconnect();
        // The claim thread is FIFO: once a later task is done, the unload queued on quit is done too.
        claims().getClaims(server.getOfflinePlayer(UUID.randomUUID())).join();
        Assertions.assertFalse(claimFile(player.getUniqueId()).isLoaded(), "dropped when the player leaves");
        Assertions.assertEquals(1, claims().getClaims(player).join().size());
    }
}
