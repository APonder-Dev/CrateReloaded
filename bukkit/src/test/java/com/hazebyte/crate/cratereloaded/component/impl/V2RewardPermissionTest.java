package com.hazebyte.crate.cratereloaded.component.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hazebyte.crate.cratereloaded.model.RewardV2;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** V2 reward permissions must mean the same as the legacy {@code permission:(node)} tag. */
class V2RewardPermissionTest {

    private static Player playerWith(String... permissions) {
        Player player = mock(Player.class);
        for (String permission : permissions) {
            when(player.hasPermission(permission)).thenReturn(true);
        }
        return player;
    }

    @Test
    void exclusivePermission_excludesPlayersWhoHaveIt() {
        // What a legacy `permission:(crates.got.wings)` reward becomes after mapping.
        RewardV2 reward = RewardV2.builder()
                .exclusivePermissions(Collections.singletonList("crates.got.wings"))
                .build();
        Assertions.assertFalse(GenerateCratePrizeComponentImpl.isAllowed(reward, playerWith("crates.got.wings")));
        Assertions.assertTrue(GenerateCratePrizeComponentImpl.isAllowed(reward, playerWith()), "was inverted");
    }

    @Test
    void inclusivePermissions_requireAll() {
        RewardV2 reward = RewardV2.builder()
                .inclusivePermissions(Arrays.asList("rank.vip", "rank.mvp"))
                .build();
        Assertions.assertTrue(GenerateCratePrizeComponentImpl.isAllowed(reward, playerWith("rank.vip", "rank.mvp")));
        Assertions.assertFalse(GenerateCratePrizeComponentImpl.isAllowed(reward, playerWith("rank.vip")));
        Assertions.assertFalse(GenerateCratePrizeComponentImpl.isAllowed(reward, playerWith()));
    }

    @Test
    void noPermissions_isAvailableToEveryone() {
        Assertions.assertTrue(GenerateCratePrizeComponentImpl.isAllowed(RewardV2.builder().build(), playerWith()));
    }
}
