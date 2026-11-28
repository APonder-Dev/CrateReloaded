package com.hazebyte.crate.cratereloaded.claim.storage.yaml;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.api.util.ItemBuilder;
import com.hazebyte.crate.cratereloaded.claim.ClaimExecutor;
import com.hazebyte.crate.cratereloaded.claim.CrateClaim;
import com.hazebyte.crate.cratereloaded.component.OpenCrateComponent;
import com.hazebyte.crate.cratereloaded.model.Config;
import com.hazebyte.crate.cratereloaded.model.RewardImpl;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** #600 / #601 follow-ups: a bad reward or a racing delete must never crash claiming or destroy a claim. */
class ClaimRobustnessTest extends BukkitTest {

    @Test
    void hideTrue_worksOnThisServer_andHideFalseHidesNothing() {
        // hideAll named ItemFlag.HIDE_POTION_EFFECTS, which no longer exists on 26.x: NoSuchFieldError.
        ItemStack hidden = new ItemBuilder(Material.DIAMOND_SWORD).hideAll(true).asItemStack();
        Assertions.assertFalse(hidden.getItemMeta().getItemFlags().isEmpty());
        ItemStack shown = new ItemBuilder(Material.DIAMOND_SWORD).hideAll(false).asItemStack();
        Assertions.assertTrue(shown.getItemMeta().getItemFlags().isEmpty(), "hide:false used to hide everything");
    }

    @Test
    void claimWithANullReward_stillPaysTheRest() {
        OpenCrateComponent open = mock(OpenCrateComponent.class);
        ClaimExecutor executor = new ClaimExecutor(plugin, open, mock(Logger.class));
        PlayerMock player = addPlayerAndSettleClaims();
        Reward good = new RewardImpl("chance:(0), cmd:(/say hi)");
        CrateClaim claim = CrateClaim.builder()
                .owner(player)
                .rewards(Arrays.asList(null, good))
                .executor(executor)
                .build();

        Assertions.assertDoesNotThrow(() -> executor.apply(claim));
        verify(open, times(1)).executeReward(any(Player.class), any(Reward.class));
    }

    @Test
    void claimWhoseRewardsNoLongerParse_isKeptOnDisk() throws Exception {
        PlayerMock player = addPlayerAndSettleClaims();
        Config config = plugin.getClaimManager() == null ? null : configFor(player);
        Assertions.assertNotNull(config);
        String key = UUID.randomUUID().toString();
        config.getConfig().set(String.format(YamlClaimConstants.CONFIG_VERSION_KEY, key), "v2");
        config.getConfig().set(String.format(YamlClaimConstants.CONFIG_TIMESTAMP_KEY, key), "1711458318507");
        config.getConfig()
                .set(
                        String.format(YamlClaimConstants.CONFIG_PLAYER_KEY, key),
                        player.getUniqueId().toString());
        config.getConfig()
                .set(
                        String.format(YamlClaimConstants.CONFIG_REWARDS_KEY, key),
                        Collections.singletonList("chance:(0), display:(NOT_A_REAL_MATERIAL 1)"));

        Collection<?> claims = plugin.getClaimManager().getClaims(player).join();

        Assertions.assertTrue(claims.isEmpty(), "an unreadable claim is not shown");
        Assertions.assertTrue(
                config.getConfig().isSet(key), "...but it is kept on disk instead of being deleted for good");
    }

    private static Config configFor(Player player) {
        return com.hazebyte.crate.cratereloaded.CorePlugin.getJavaPluginComponent()
                .getConfigManagerComponent()
                .addConfig(new java.io.File(plugin.getDataFolder(), "claim/" + player.getUniqueId() + ".yml"));
    }
}
