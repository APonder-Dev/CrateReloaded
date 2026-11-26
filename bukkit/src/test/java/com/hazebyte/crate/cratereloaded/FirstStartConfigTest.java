package com.hazebyte.crate.cratereloaded;

import com.hazebyte.crate.BukkitTest;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** A first start (empty data folder) must read the bundled config.yml, not built-in fallbacks. */
class FirstStartConfigTest extends BukkitTest {

    @Test
    void firstStart_usesTheBundledConfig() {
        Assertions.assertTrue(new File(plugin.getDataFolder(), "config.yml").exists());
        // Bundled value; the built-in fallback used before the default file existed is "[CrateReloaded] ".
        Assertions.assertEquals("&8[&bCrates&8]&e", plugin.getSettings().getPluginMessagePrefix());
        Assertions.assertTrue(plugin.getSettings().isHandlingClaims());
    }
}
