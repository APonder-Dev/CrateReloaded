package com.hazebyte.crate.cratereloaded.util;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public class Enchantments {
    private static final Map<String, Enchantment> ENCHANTS = new HashMap<>();
    private static final StringJoiner enchantSB = new StringJoiner(", ");

    static {
        for (Enchantment e : Enchantment.values()) enchantSB.add(e.getName());

        put("environmental_protection", "protection");
        put("protection", "protection");
        put("fire_protection", "fire_protection");
        put("fall_protection", "feather_falling");
        put("explosion_protection", "blast_protection");
        put("projectile_protection", "projectile_protection");
        put("oxygen", "respiration");
        put("water_worker", "aqua_affinity");
        put("thorns", "thorns");
        put("depth_strider", "depth_strider");
        put("frost_walker", "frost_walker");
        put("binding_curse", "binding_curse");
        put("sharpness", "sharpness");
        put("smite", "smite");
        put("bane_of_arthropods", "bane_of_arthropods");
        put("fire_aspect", "fire_aspect");
        put("looting", "looting");
        put("sweeping_edge", "sweeping_edge", "sweeping"); // key was "sweeping" before 1.20.5
        put("efficiency", "efficiency");
        put("silk_touch", "silk_touch");
        put("unbreaking", "unbreaking");
        put("fortune", "fortune");
        put("power", "power");
        put("punch", "punch");
        put("flame", "flame");
        put("infinity", "infinity");
        put("luck", "luck_of_the_sea");
        put("lure", "lure");
        put("mending", "mending");
        put("vanishing_curse", "vanishing_curse");

        put("channeling", "channeling");
        put("impaling", "impaling");
        put("multishot", "multishot"); // was misspelt "MULTSHOT", so it never resolved
        put("piercing", "piercing");
        put("quick_charge", "quick_charge");

        // Pre-1.20.5 constant names Minecraft renamed; kept so configs written before the rename resolve.
        put("durability", "unbreaking");
        put("dig_speed", "efficiency");
        put("damage_all", "sharpness");
        put("damage_undead", "smite");
        put("damage_arthropods", "bane_of_arthropods");
        put("loot_bonus_blocks", "fortune");
        put("loot_bonus_mobs", "looting");
        put("arrow_damage", "power");
        put("arrow_knockback", "punch");
        put("arrow_fire", "flame");
        put("arrow_infinite", "infinity");
        put("protection_environmental", "protection");
        put("protection_fire", "fire_protection");
        put("protection_fall", "feather_falling");
        put("protection_explosions", "blast_protection");
        put("protection_projectile", "projectile_protection");
    }

    /**
     * Maps a config alias to the first enchantment found among the given {@code minecraft:} keys. Keys are
     * stable across versions, unlike the Bukkit constant names (renamed in 1.20.5), so the same lookup works
     * on every build family. Enchantments this server does not have are skipped.
     */
    private static void put(String name, String... keys) {
        for (String key : keys) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key));
            if (enchantment != null) {
                ENCHANTS.put(name, enchantment);
                return;
            }
        }
    }

    public static Enchantment getByName(String name) {
        Enchantment enchantment = Enchantment.getByName(name.toUpperCase());
        return enchantment == null ? ENCHANTS.get(name.toLowerCase()) : enchantment;
    }

    public static String getStringFormat() {
        return enchantSB.toString();
    }
}
