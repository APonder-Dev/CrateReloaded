package com.hazebyte.crate.cratereloaded.compat;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.cratereloaded.util.item.PotionUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PotionCompatTest extends BukkitTest {

    @Test
    void type_returnsFirstExistingConstant() {
        Assertions.assertEquals(PotionType.SWIFTNESS, PotionCompat.type("NOT_A_TYPE", "SWIFTNESS", "SPEED"));
        Assertions.assertNull(PotionCompat.type("NOT_A_TYPE"));
    }

    @Test
    void modernServer_hasBasePotionType() {
        // The suite runs on MockBukkit 1.21 / 26.x; the 1.20.1 fallback path is covered by the mc120 compile.
        Assertions.assertTrue(PotionCompat.hasBasePotionType());
    }

    @Test
    void setBase_selectsStrongAndLongVariants() {
        Assertions.assertEquals(PotionType.STRONG_SWIFTNESS, base(PotionType.SWIFTNESS, false, true));
        Assertions.assertEquals(PotionType.LONG_SWIFTNESS, base(PotionType.SWIFTNESS, true, false));
        Assertions.assertEquals(PotionType.SWIFTNESS, base(PotionType.SWIFTNESS, false, false));
        // No LONG_ variant exists for HEALING: keep the base type rather than fail.
        Assertions.assertEquals(PotionType.HEALING, base(PotionType.HEALING, true, false));
    }

    @Test
    void potionUtil_toItemStack_levelTwoIsStrongNotLong() {
        // Regression: toItemStack used to pass (upgraded, extended) into PotionData(type, extended, upgraded).
        ItemStack item = new PotionUtil(PotionType.SWIFTNESS, 2).toItemStack(1);
        Assertions.assertEquals(PotionType.STRONG_SWIFTNESS, ((PotionMeta) item.getItemMeta()).getBasePotionType());
        // The extended path goes through PotionType#isInstant, which MockBukkit does not implement;
        // setBase_selectsStrongAndLongVariants covers the LONG_ mapping directly.
    }

    private static PotionType base(PotionType type, boolean extended, boolean upgraded) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        PotionCompat.setBase(meta, type, extended, upgraded);
        return meta.getBasePotionType();
    }
}
