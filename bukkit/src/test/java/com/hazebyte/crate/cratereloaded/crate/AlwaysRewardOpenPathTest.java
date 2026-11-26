package com.hazebyte.crate.cratereloaded.crate;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.component.model.CrateOpenRequest;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** #494 / #397 through the real open path: the always:() reward's command must run. */
class AlwaysRewardOpenPathTest extends BukkitTest {

    @Test
    void instantKeyOpening_runsTheAlwaysCommand() {
        List<String> commands = new ArrayList<>();
        Listener spy = new Listener() {
            @EventHandler
            public void onCommand(ServerCommandEvent event) {
                commands.add(event.getCommand());
            }
        };
        Bukkit.getPluginManager().registerEvents(spy, plugin);
        try {
            PlayerMock player = server.addPlayer();
            Crate crate = plugin.getCrateRegistrar().getCrate("FoodKey");
            CorePlugin.getJavaPluginComponent()
                    .getOpenCrateComponent()
                    .openCrate(CrateOpenRequest.builder()
                            .player(player)
                            .location(player.getLocation())
                            .crate(crate)
                            // Exactly what CrateHandler.open sends: the V2 crate from storage is primary.
                            .crateV2(((CrateHandler) plugin.getCrateRegistrar()).getCrateV2("FoodKey"))
                            .build());
            server.getScheduler().performTicks(10);
            Assertions.assertTrue(
                    commands.stream().anyMatch(c -> c.startsWith("heal ")),
                    "always:() cmd:(heal {player}) must run; commands seen: " + commands);
        } finally {
            HandlerList.unregisterAll(spy);
        }
    }
}
