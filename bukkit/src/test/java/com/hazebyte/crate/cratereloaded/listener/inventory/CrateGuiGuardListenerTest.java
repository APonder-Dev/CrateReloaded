package com.hazebyte.crate.cratereloaded.listener.inventory;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.cratereloaded.menu.BaseHolder;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** #475 / #573: nothing may be taken out of, or put into, a CrateReloaded GUI. */
class CrateGuiGuardListenerTest extends BukkitTest {

    /** Stands in for a third-party plugin that un-cancels clicks (the #475 report). */
    public static class UncancellingListener implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(false);
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onDrag(InventoryDragEvent event) {
            event.setCancelled(false);
        }
    }

    private static boolean hostileRegistered;
    private PlayerMock player;

    @BeforeEach
    void setUpPlayer() {
        if (!hostileRegistered) {
            Bukkit.getPluginManager().registerEvents(new UncancellingListener(), plugin);
            hostileRegistered = true;
        }
        player = server.addPlayer();
    }

    private InventoryView openCrateGui() {
        BaseHolder holder = new BaseHolder(null);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Crate");
        inventory.setItem(13, new ItemStack(Material.TOTEM_OF_UNDYING));
        return player.openInventory(inventory);
    }

    @Test
    void shiftClickOutOfCrateGui_isCancelled_evenIfAnotherPluginUncancels() {
        InventoryView view = openCrateGui();
        InventoryClickEvent click = new InventoryClickEvent(
                view,
                InventoryType.SlotType.CONTAINER,
                13,
                ClickType.SHIFT_LEFT,
                InventoryAction.MOVE_TO_OTHER_INVENTORY);
        Bukkit.getPluginManager().callEvent(click);
        Assertions.assertTrue(click.isCancelled());
    }

    @Test
    void dragIntoCrateGui_isCancelled() {
        InventoryView view = openCrateGui();
        InventoryDragEvent drag = new InventoryDragEvent(
                view,
                new ItemStack(Material.DIRT),
                new ItemStack(Material.DIRT, 2),
                false,
                Collections.singletonMap(0, new ItemStack(Material.DIRT)));
        Bukkit.getPluginManager().callEvent(drag);
        Assertions.assertTrue(drag.isCancelled());
    }

    @Test
    void clicksInOtherInventories_areLeftAlone() {
        InventoryView view = player.openInventory(Bukkit.createInventory(null, 27, "Chest"));
        InventoryClickEvent click = new InventoryClickEvent(
                view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        Bukkit.getPluginManager().callEvent(click);
        Assertions.assertFalse(click.isCancelled());
    }
}
