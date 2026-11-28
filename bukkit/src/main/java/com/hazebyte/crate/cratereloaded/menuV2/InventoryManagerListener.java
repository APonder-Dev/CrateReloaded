package com.hazebyte.crate.cratereloaded.menuV2;

import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.server.PluginDisableEvent;

public class InventoryManagerListener implements Listener {

    private InventoryManager inventoryManager;
    private InventoryHistoryManager inventoryHistoryManager;

    public InventoryManagerListener(
            @NonNull InventoryManager inventoryManager, @NonNull InventoryHistoryManager inventoryHistoryManager) {
        this.inventoryManager = inventoryManager;
        this.inventoryHistoryManager = inventoryHistoryManager;
    }

    // HIGH, not HIGHEST: CrateGuiGuardListener cancels every V2 GUI click at HIGHEST, after the buttons ran.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        inventoryManager.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        inventoryManager.handleOpen(event);
        inventoryHistoryManager.handleOpen(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        inventoryManager.handleClose(event);
        inventoryHistoryManager.handleClose(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginDisable(PluginDisableEvent event) {
        // This fires for other plugins only (ours is unregistered by then; CorePlugin#onDisable closes our GUIs),
        // so it must not close every crate GUI whenever some unrelated plugin is disabled.
        if (!event.getPlugin().getName().equals("CrateReloaded")) {
            return;
        }
        inventoryManager.closeAllInventories();
        inventoryHistoryManager.handlePluginDisable(event);
    }
}
