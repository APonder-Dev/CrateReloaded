package com.hazebyte.crate.cratereloaded.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/**
 * Potion API differences between the build families.
 *
 * <p>Minecraft 1.20.5 renamed several {@link PotionType} constants ({@code SPEED} became {@code SWIFTNESS}
 * and so on) and moved the long/strong variants into their own types ({@code LONG_SWIFTNESS},
 * {@code STRONG_SWIFTNESS}). {@code PotionMeta#setBasePotionType} does not exist before 1.20.2, and
 * {@link PotionMeta#setBasePotionData(PotionData)} is deprecated on newer servers. Lookups here go by
 * name and the newer method is found once at class load, so the same bytecode runs on every family.
 */
public final class PotionCompat {

    /** {@code PotionMeta#setBasePotionType(PotionType)}, or {@code null} before 1.20.2. */
    private static final MethodHandle SET_BASE_POTION_TYPE = findSetBasePotionType();

    private PotionCompat() {}

    private static MethodHandle findSetBasePotionType() {
        try {
            return MethodHandles.publicLookup()
                    .findVirtual(
                            PotionMeta.class, "setBasePotionType", MethodType.methodType(void.class, PotionType.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    /** @return whether this server has the 1.20.2+ base potion type API. */
    public static boolean hasBasePotionType() {
        return SET_BASE_POTION_TYPE != null;
    }

    /**
     * Returns the first {@link PotionType} constant that exists on this server.
     *
     * @param names candidate constant names, modern name first
     * @return the matching type, or {@code null} if none exists
     */
    public static PotionType type(String... names) {
        for (String name : names) {
            try {
                return PotionType.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // Try the next candidate.
            }
        }
        return null;
    }

    /**
     * Sets the base potion of {@code meta}.
     *
     * @param meta the meta to change
     * @param type the unmodified base type, e.g. {@code SWIFTNESS}
     * @param extended whether the long-duration variant is wanted
     * @param upgraded whether the level II variant is wanted
     */
    public static void setBase(PotionMeta meta, PotionType type, boolean extended, boolean upgraded) {
        if (SET_BASE_POTION_TYPE == null) {
            meta.setBasePotionData(new PotionData(type, extended, upgraded));
            return;
        }
        PotionType resolved = type;
        if (upgraded) {
            PotionType strong = type("STRONG_" + type.name());
            resolved = strong != null ? strong : type;
        } else if (extended) {
            PotionType lng = type("LONG_" + type.name());
            resolved = lng != null ? lng : type;
        }
        try {
            SET_BASE_POTION_TYPE.invoke(meta, resolved);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("setBasePotionType failed", e);
        }
    }
}
