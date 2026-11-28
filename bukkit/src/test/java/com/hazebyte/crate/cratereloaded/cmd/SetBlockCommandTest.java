package com.hazebyte.crate.cratereloaded.cmd;

import com.hazebyte.crate.BukkitTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** /crate set on a block that already has the crate says so, instead of reporting an error. */
class SetBlockCommandTest extends BukkitTest {

    private static String messages(PlayerMock player) {
        StringBuilder all = new StringBuilder();
        for (String message = player.nextMessage(); message != null; message = player.nextMessage()) {
            all.append(message).append(" | ");
        }
        return all.toString();
    }

    @Test
    void settingTheSameCrateTwice_saysAlreadySet() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        Location block = player.getLocation().clone().add(2, 0, 0);
        block.getBlock().setType(Material.CHEST);
        String target = String.format(
                "%s:%d,%d,%d", block.getWorld().getName(), block.getBlockX(), block.getBlockY(), block.getBlockZ());

        player.performCommand("crate set FoodKey " + target);
        String first = messages(player);
        player.performCommand("crate set FoodKey " + target);
        String second = messages(player);

        // On the mock server messages are their keys.
        Assertions.assertTrue(first.contains("core.successful_block_set"), first);
        Assertions.assertTrue(second.contains("core.block_already_set"), second);
        Assertions.assertFalse(second.contains("core.invalid_block_set"), second);
    }
}
