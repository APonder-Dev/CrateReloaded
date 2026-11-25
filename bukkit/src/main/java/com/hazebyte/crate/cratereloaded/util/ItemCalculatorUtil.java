package com.hazebyte.crate.cratereloaded.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemCalculatorUtil {

    /**
     * The stack size an item may legally occupy in one slot: 1 for totems, tools, armour, shulker boxes,
     * cake and so on, 16 for ender pearls, 64 for most blocks, or a custom {@code max_stack_size} on 1.20.5+.
     * Using a fixed 64 was issue #486 (unstackable rewards handed out as one stack).
     *
     * @param item the item
     * @return the per-slot stack size, at least 1
     */
    public static int maxStackSize(@NonNull ItemStack item) {
        return Math.max(1, item.getMaxStackSize());
    }

    /**
     * Return the number of slots required for an itemstack to be put into an inventory, honouring the
     * item's own max stack size. E.g. 65 stone needs 2 slots; 3 totems need 3 slots.
     *
     * @param item the item
     * @return the number of slots needed for an itemstack.
     */
    public static int getNumberOfSlotsRequired(@NonNull ItemStack item) {
        return (int) Math.ceil((double) item.getAmount() / maxStackSize(item));
    }

    /**
     * Splits an item into stacks no larger than its max stack size.
     *
     * @param item the item, possibly with an amount above its max stack size
     * @return legal stacks whose amounts add up to {@code item.getAmount()}
     */
    public static List<ItemStack> split(@NonNull ItemStack item) {
        int stack = maxStackSize(item);
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = item.getAmount();
        while (remaining > 0) {
            ItemStack part = item.clone();
            part.setAmount(Math.min(stack, remaining));
            remaining -= part.getAmount();
            stacks.add(part);
        }
        return stacks;
    }

    /**
     * Puts as much of {@code item} into the player's empty slots as fits, one legal stack per slot.
     *
     * @param player the player
     * @param item the item; its amount may exceed the max stack size
     * @return what could not be given (amount may again exceed the max stack size), or empty if all of it
     *     was given
     */
    public static Optional<ItemStack> putItemsIntoInventory(
            @NonNull final Player player, @NonNull final ItemStack item) {
        int remaining = item.getAmount();
        int emptySlots = PlayerUtil.getSlotsLeft(player);
        int stack = maxStackSize(item);

        while (remaining > 0 && emptySlots > 0) {
            ItemStack part = item.clone();
            part.setAmount(Math.min(stack, remaining));
            remaining -= part.getAmount();
            emptySlots--;
            // addItem reports what it could not place; count it back instead of losing it.
            Map<Integer, ItemStack> notAdded = player.getInventory().addItem(part);
            for (ItemStack rejected : notAdded.values()) {
                remaining += rejected.getAmount();
            }
            if (!notAdded.isEmpty()) {
                break;
            }
        }

        if (remaining <= 0) {
            return Optional.empty();
        }
        ItemStack leftOver = item.clone();
        leftOver.setAmount(remaining);
        return Optional.of(leftOver);
    }

    /**
     * Drops an item at a location as legal stacks, so picking it up can't produce an oversized stack.
     *
     * @param location where to drop
     * @param item the item; its amount may exceed the max stack size
     */
    public static void dropNaturally(@NonNull Location location, @NonNull ItemStack item) {
        for (ItemStack part : split(item)) {
            location.getWorld().dropItemNaturally(location, part);
        }
    }
}
