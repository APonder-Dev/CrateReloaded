package com.hazebyte.crate.cratereloaded.crate.animation;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.api.event.CrateRewardEvent;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.component.model.CrateOpenRequest;
import com.hazebyte.crate.cratereloaded.crate.CrateHandler;
import com.hazebyte.crate.cratereloaded.crate.OpeningLedger;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The prize saved when an animated opening starts (paid if the player leaves) must be the reward the animation
 * lands on. It used to be a different roll, so leaving after seeing a poor result re-rolled the prize.
 */
class LandingPrizeTest extends BukkitTest {

    private static final int RUNS = 15;

    @ParameterizedTest
    @ValueSource(strings = {"FoodKeyT2", "FoodKeyT3", "FoodKeyT4", "FoodKeyT5", "FoodKeyT6", "FoodKeyT7", "FoodKeyT8"})
    void savedPrize_isTheRewardTheAnimationLandsOn(String crateName) {
        List<List<Reward>> delivered = new ArrayList<>();
        Listener spy = new Listener() {
            @EventHandler
            public void onReward(CrateRewardEvent event) {
                delivered.add(new ArrayList<>(event.getRewards()));
            }
        };
        Bukkit.getPluginManager().registerEvents(spy, plugin);
        try {
            Crate crate = plugin.getCrateRegistrar().getCrate(crateName);
            OpeningLedger ledger = plugin.getOpeningLedger();
            for (int run = 0; run < RUNS; run++) {
                PlayerMock player = server.addPlayer();
                player.setOp(true);
                delivered.clear();
                CorePlugin.getJavaPluginComponent()
                        .getOpenCrateComponent()
                        .openCrate(CrateOpenRequest.builder()
                                .player(player)
                                .location(player.getLocation())
                                .crate(crate)
                                .crateV2(((CrateHandler) plugin.getCrateRegistrar()).getCrateV2(crateName))
                                .build());
                OpeningLedger.Session session =
                        ledger.session(player.getUniqueId()).orElseThrow(AssertionError::new);
                Reward saved = session.getPrize().get(0);

                server.getScheduler().performTicks(600);

                Assertions.assertEquals(1, delivered.size(), crateName + ": one delivery per opening");
                Assertions.assertSame(
                        saved,
                        delivered.get(0).get(0),
                        crateName + " run " + run + ": the saved prize must be the reward the animation landed on");
                Assertions.assertFalse(ledger.hasSession(player.getUniqueId()));
            }
        } finally {
            HandlerList.unregisterAll(spy);
        }
    }
}
