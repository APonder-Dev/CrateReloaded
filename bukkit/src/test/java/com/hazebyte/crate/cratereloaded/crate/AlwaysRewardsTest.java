package com.hazebyte.crate.cratereloaded.crate;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.crate.reward.Reward;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** #494 / #397: always:() rewards in reward.rewards must actually be given. */
class AlwaysRewardsTest extends BukkitTest {

    @Test
    void alwaysRewards_areSeparatedIntoTheConstantList() {
        List<Crate> crates = plugin.getCrateRegistrar().getCrates();
        Assertions.assertFalse(crates.isEmpty());
        for (Crate crate : crates) {
            // Every bundled crate has exactly one always:() reward (the heal command).
            Assertions.assertEquals(1, crate.getConstantRewards().size(), crate.getCrateName() + " constant list");
            Assertions.assertTrue(
                    crate.getRewards().stream().noneMatch(Reward::isConstant),
                    crate.getCrateName() + ": always rewards must not sit in the random pool with chance 0");
        }
    }

    @Test
    void instantOpening_includesTheAlwaysReward() {
        Crate crate = plugin.getCrateRegistrar().getCrate("FoodKey");
        PlayerMock player = server.addPlayer();
        List<Reward> rewards = plugin.getCrateRegistrar().generateCrateRewards(crate, player);
        Assertions.assertTrue(rewards.stream().anyMatch(Reward::isConstant), "always reward is given");
        Assertions.assertEquals(4, rewards.size(), "3 random rewards + 1 always reward");
    }
}
