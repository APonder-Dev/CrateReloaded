package com.hazebyte.crate.cratereloaded.crate;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.crate.AnimationType;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.crate.CrateType;
import com.hazebyte.crate.api.crate.EndAnimationType;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The bundled crate.yml demonstrates every crate type and animation that config can select. */
class BundledCratesTest extends BukkitTest {

    private static Crate crate(String name) {
        Crate crate = plugin.getCrateRegistrar().getCrate(name);
        Assertions.assertNotNull(crate, name + " is loaded");
        return crate;
    }

    @Test
    void everyAnimationHasAnExample() {
        Assertions.assertEquals(AnimationType.NONE, crate("FoodKey").getAnimationType());
        Assertions.assertEquals(AnimationType.ROULETTE, crate("FoodKeyT2").getAnimationType());
        Assertions.assertEquals(AnimationType.CSGO, crate("FoodKeyT3").getAnimationType());
        Assertions.assertEquals(AnimationType.CSGO_REVERSE, crate("FoodKeyT4").getAnimationType());
        Assertions.assertEquals(AnimationType.RECTANGLE, crate("FoodKeyT5").getAnimationType());
        Assertions.assertEquals(AnimationType.RECTANGLE_REVERSE, crate("FoodKeyT6").getAnimationType());
        Assertions.assertEquals(AnimationType.HEART, crate("FoodKeyT7").getAnimationType());
        Assertions.assertEquals(EndAnimationType.RANDOM, crate("FoodKeyT8").getEndAnimationType());
    }

    @Test
    void everyCrateTypeHasAnExample() {
        Assertions.assertEquals(CrateType.SUPPLY, crate("FoodChest").getType());
        Assertions.assertEquals(CrateType.MYSTERY, crate("FoodPack").getType());
        Assertions.assertEquals(CrateType.KEY, crate("FoodKey").getType());
        Assertions.assertEquals(CrateType.MYSTERY, crate("FoodBox").getType());
        Assertions.assertEquals(AnimationType.ROULETTE, crate("FoodBox").getAnimationType(), "animated mystery");
    }

    @Test
    void everyKeyItemIsUniqueAndRecognised() {
        Set<String> seen = new HashSet<>();
        for (Crate crate : plugin.getCrateRegistrar().getCrates()) {
            ItemStack item = crate.getItem();
            Assertions.assertTrue(seen.add(item.getItemMeta().getDisplayName()), "duplicate item: " + crate.getCrateName());
            Assertions.assertSame(crate, plugin.getCrateRegistrar().getCrate(item), crate.getCrateName() + " item maps back");
        }
        Assertions.assertEquals(11, seen.size());
    }
}
