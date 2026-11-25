package com.hazebyte.crate.cratereloaded.util.item;

import com.hazebyte.crate.cratereloaded.compat.PotionCompat;
import java.util.Collection;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionBrewer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

public class PotionUtil {
    private boolean extended = false;
    private boolean splash = false;
    private int level = 1;
    private PotionType type;

    /**
     * Construct a new potion of the given type. Unless the type is {@link PotionType#WATER}, it will
     * be level one, without extended duration. Don't use this constructor to create a no-effect
     * potion other than water bottle.
     *
     * @param type The potion type
     */
    public PotionUtil(PotionType type) {
        Validate.notNull(type, "Null PotionType");
        this.type = type;
    }

    /**
     * Create a new potion of the given type and level.
     *
     * @param type The type of potion.
     * @param level The potion's level.
     */
    public PotionUtil(PotionType type, int level) {
        this(type);
        Validate.notNull(type, "Type cannot be null");
        Validate.isTrue(level > 0 && level < 3, "Level must be 1 or 2");
        this.level = level;
    }

    /**
     * Create a new potion of the given type and level.
     *
     * @param type The type of potion.
     * @param level The potion's level.
     * @param splash Whether it is a splash potion.
     * @deprecated In favour of using {@link #PotionUtil(PotionType)} with {@link #splash()}.
     */
    @Deprecated
    public PotionUtil(PotionType type, int level, boolean splash) {
        this(type, level);
        this.splash = splash;
    }

    /**
     * Create a new potion of the given type and level.
     *
     * @param type The type of potion.
     * @param level The potion's level.
     * @param splash Whether it is a splash potion.
     * @param extended Whether it has an extended duration.
     * @deprecated In favour of using {@link #PotionUtil(PotionType)} with {@link #extend()} and
     *     possibly {@link #splash()}.
     */
    @Deprecated
    public PotionUtil(PotionType type, int level, boolean splash, boolean extended) {
        this(type, level, splash);
        this.extended = extended;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public PotionUtil(int name) {
        this(PotionType.WATER);
    }

    /**
     * Chain this to the constructor to make the potion a splash potion.
     *
     * @return The potion.
     */
    public PotionUtil splash() {
        setSplash(true);
        return this;
    }

    /**
     * Chain this to the constructor to extend the potion's duration.
     *
     * @return The potion.
     */
    public PotionUtil extend() {
        setHasExtendedDuration(true);
        return this;
    }

    /**
     * Applies the effects of this potion to the given {@link ItemStack}. The ItemStack must be a
     * potion.
     *
     * @param to The itemstack to apply to
     */
    public void apply(ItemStack to) {
        Validate.notNull(to, "itemstack cannot be null");
        Validate.isTrue(to.hasItemMeta(), "given itemstack is not a potion");
        Validate.isTrue(to.getItemMeta() instanceof PotionMeta, "given itemstack is not a potion");
        PotionMeta meta = (PotionMeta) to.getItemMeta();
        PotionCompat.setBase(meta, type, extended, level == 2);
        to.setItemMeta(meta);
    }

    /**
     * Applies the effects that would be applied by this potion to the given {@link LivingEntity}.
     *
     * @see LivingEntity#addPotionEffects(Collection)
     * @param to The entity to apply the effects to
     */
    public void apply(LivingEntity to) {
        Validate.notNull(to, "entity cannot be null");
        to.addPotionEffects(getEffects());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PotionUtil other = (PotionUtil) obj;
        return extended == other.extended && splash == other.splash && level == other.level && type == other.type;
    }

    /**
     * Returns a collection of {@link PotionEffect}s that this {@link PotionUtil} would confer upon a
     * {@link LivingEntity}.
     *
     * @see PotionBrewer#getEffectsFromDamage(int)
     * @return The effects that this potion applies
     */
    public Collection<PotionEffect> getEffects() {
        return getBrewer().getEffects(type, level == 2, extended);
    }

    /**
     * Returns the level of this potion.
     *
     * @return The level of this potion
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the {@link PotionType} of this potion.
     *
     * @return The type of this potion
     */
    public PotionType getType() {
        return type;
    }

    /**
     * Returns whether this potion has an extended duration.
     *
     * @return Whether this potion has extended duration
     */
    public boolean hasExtendedDuration() {
        return extended;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime + level;
        result = prime * result + (extended ? 1231 : 1237);
        result = prime * result + (splash ? 1231 : 1237);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /**
     * Returns whether this potion is a splash potion.
     *
     * @return Whether this is a splash potion
     */
    public boolean isSplash() {
        return splash;
    }

    /**
     * Set whether this potion has extended duration. This will cause the potion to have roughly 8/3
     * more duration than a regular potion.
     *
     * @param isExtended Whether the potion should have extended duration
     */
    public void setHasExtendedDuration(boolean isExtended) {
        Validate.isTrue(type == null || !type.isInstant(), "Instant potions cannot be extended");
        extended = isExtended;
    }

    /**
     * Sets whether this potion is a splash potion. Splash potions can be thrown for a radius effect.
     *
     * @param isSplash Whether this is a splash potion
     */
    public void setSplash(boolean isSplash) {
        splash = isSplash;
    }

    /**
     * Sets the {@link PotionType} of this potion.
     *
     * @param type The new type of this potion
     */
    public void setType(PotionType type) {
        this.type = type;
    }

    /**
     * Sets the level of this potion.
     *
     * @param level The new level of this potion
     */
    public void setLevel(int level) {
        Validate.notNull(this.type, "No-effect potions don't have a level.");
        Validate.isTrue(level > 0 && level <= 2, "Level must be between 1 and 2 for this potion");
        this.level = level;
    }

    /**
     * Converts this potion to a valid potion damage short, usable for potion item stacks.
     *
     * @return The damage value of this potion
     * @deprecated Non-functional
     */
    @Deprecated
    public short toDamageValue() {
        return 0;
    }

    /**
     * Converts this potion to an {@link ItemStack} with the specified amount and a correct damage
     * value.
     *
     * @param amount The amount of the ItemStack
     * @return The created ItemStack
     */
    public ItemStack toItemStack(int amount) {
        Material material;
        if (isSplash()) {
            material = Material.SPLASH_POTION;
        } else {
            material = Material.POTION;
        }
        ItemStack itemStack = new ItemStack(material, amount);
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        // Previously PotionData(type, upgraded, extended): the two flags were swapped.
        PotionCompat.setBase(meta, type, extended, level == 2);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private static PotionBrewer brewer;

    private static final int EXTENDED_BIT = 0x40;
    private static final int POTION_BIT = 0xF;
    private static final int SPLASH_BIT = 0x4000;
    private static final int TIER_BIT = 0x20;
    private static final int TIER_SHIFT = 5;

    /**
     * @param damage the damage value
     * @return the produced potion
     */
    public static PotionUtil fromDamage(int damage) {
        PotionType type;
        switch (damage & POTION_BIT) {
            case 1:
                type = PotionCompat.type("REGENERATION", "REGEN");
                break;
            case 2:
                type = PotionCompat.type("SWIFTNESS", "SPEED");
                break;
            case 3:
                type = PotionCompat.type("FIRE_RESISTANCE");
                break;
            case 4:
                type = PotionCompat.type("POISON");
                break;
            case 5:
                type = PotionCompat.type("HEALING", "INSTANT_HEAL");
                break;
            case 6:
                type = PotionCompat.type("NIGHT_VISION");
                break;
            case 8:
                type = PotionCompat.type("WEAKNESS");
                break;
            case 9:
                type = PotionCompat.type("STRENGTH");
                break;
            case 10:
                type = PotionCompat.type("SLOWNESS");
                break;
            case 11:
                type = PotionCompat.type("LEAPING", "JUMP");
                break;
            case 12:
                type = PotionCompat.type("HARMING", "INSTANT_DAMAGE");
                break;
            case 13:
                type = PotionCompat.type("WATER_BREATHING");
                break;
            case 14:
                type = PotionCompat.type("INVISIBILITY");
                break;
            default:
                type = PotionType.WATER;
        }
        PotionUtil potion;
        if (type == PotionType.WATER) {
            potion = new PotionUtil(PotionType.WATER);
        } else {
            int level = (damage & TIER_BIT) >> TIER_SHIFT;
            level++;
            potion = new PotionUtil(type, level);
        }
        if ((damage & SPLASH_BIT) != 0) {
            potion = potion.splash();
        }
        if ((damage & EXTENDED_BIT) != 0) {
            potion = potion.extend();
        }
        return potion;
    }

    public static PotionUtil fromItemStack(ItemStack item) {
        Validate.notNull(item, "item cannot be null");
        if (item.getType() != Material.POTION
                && item.getType() != Material.LINGERING_POTION
                && item.getType() != Material.SPLASH_POTION
                && !item.getType().name().contains("TIPPED"))
            throw new IllegalArgumentException("item is not a potion");
        return fromDamage(item.getDurability());
    }

    /**
     * Resolves a base {@link PotionType} from a configuration name, translating the pre-1.20.5 names
     * that Minecraft has since renamed ({@code SPEED} to {@code SWIFTNESS}, {@code JUMP} to
     * {@code LEAPING}, {@code INSTANT_HEAL} to {@code HEALING}, {@code INSTANT_DAMAGE} to
     * {@code HARMING}, {@code REGEN} to {@code REGENERATION}, and the removed {@code UNCRAFTABLE} to
     * {@code WATER}). This keeps crate configs written before 1.20.5 working on modern servers.
     *
     * @param name the configured potion type name
     * @return the matching {@link PotionType}, or {@code null} if the name is not recognised
     */
    public static PotionType matchType(String name) {
        switch (name.toUpperCase()) {
            // Modern name first, then the pre-1.20.5 name: whichever exists on this server wins.
            case "SPEED":
            case "SWIFTNESS":
                return PotionCompat.type("SWIFTNESS", "SPEED");
            case "JUMP":
            case "LEAPING":
                return PotionCompat.type("LEAPING", "JUMP");
            case "INSTANT_HEAL":
            case "HEALING":
                return PotionCompat.type("HEALING", "INSTANT_HEAL");
            case "INSTANT_DAMAGE":
            case "HARMING":
                return PotionCompat.type("HARMING", "INSTANT_DAMAGE");
            case "REGEN":
            case "REGENERATION":
                return PotionCompat.type("REGENERATION", "REGEN");
            case "UNCRAFTABLE":
                return PotionCompat.type("WATER");
            default:
                return PotionCompat.type(name.toUpperCase());
        }
    }

    /**
     * Returns an instance of {@link PotionBrewer}.
     *
     * @return An instance of PotionBrewer
     */
    public static PotionBrewer getBrewer() {
        return brewer;
    }

    /**
     * Sets the current instance of {@link PotionBrewer}. Generally not to be used from within a
     * plugin.
     *
     * @param other The new PotionBrewer
     */
    public static void setPotionBrewer(PotionBrewer other) {
        if (brewer != null) throw new IllegalArgumentException("brewer can only be set internally");
        brewer = other;
    }

    /**
     * @return the name id
     * @deprecated Non-functional
     */
    @Deprecated
    public int getNameId() {
        return 0;
    }
}
