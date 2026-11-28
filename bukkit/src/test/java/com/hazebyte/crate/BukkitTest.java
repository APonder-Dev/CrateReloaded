package com.hazebyte.crate;

import com.hazebyte.crate.cratereloaded.CorePlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class BukkitTest {

    protected static ServerMock server;
    protected static CorePlugin plugin;

    @BeforeAll
    protected static void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(CorePlugin.class);
    }

    /**
     * Adds a player and waits until the plugin has finished reading their claim file for the join message. Tests that
     * touch claim files from another thread must use this: the file is not safe to change from two threads at once.
     */
    protected static PlayerMock addPlayerAndSettleClaims() {
        PlayerMock player = server.addPlayer();
        plugin.getClaimRegistrar().getClaims(player).join(); // the claim thread is FIFO
        return player;
    }

    @AfterAll
    protected static void tearDown() {
        MockBukkit.unmock();
    }
}
