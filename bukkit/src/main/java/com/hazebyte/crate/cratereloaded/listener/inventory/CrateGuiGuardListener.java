package com.hazebyte.crate.cratereloaded.listener.inventory;

import com.hazebyte.crate.cratereloaded.menu.BaseHolder;
import com.hazebyte.crate.cratereloaded.menuV2.InventoryManager;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/**
 * Final word on item movement while a CrateReloaded GUI (animation, preview, claim or V2 menu) is open:
 * nothing may enter or leave it. Fixes #475 and #573.
 *
 * <p>The per-menu listeners only cancel clicks at LOW priority with {@code ignoreCancelled = true}, so when
 * another plugin had already cancelled a click, they skipped it. If yet another plugin then un-cancelled it,
 * the item moved: #475, "another plugin cancelling our plugin's action". V2 GUIs, including the
 * {@code CSGO_V2}/{@code ROULETTE_V2} animations, only cancelled clicks on button slots, so their display items
 * could be taken. Drags were not handled at all.
 *
 * <p>This listener runs at {@link EventPriority#HIGHEST} without {@code ignoreCancelled}, after every
 * button handler, and cancels unconditionally. It then resends the player's inventory a tick later, which
 * clears the client-side "ghost" item that a cancelled shift-click briefly shows (#573).
 */
public class CrateGuiGuardListener implements Listener {

    private final Plugin plugin;
    private final InventoryManager inventoryManager;

    public CrateGuiGuardListener(@NonNull Plugin plugin, @NonNull InventoryManager inventoryManager) {
        this.plugin = plugin;
        this.inventoryManager = inventoryManager;
    }

    /**
     * @param top the top inventory of a view
     * @return whether it is a GUI owned by CrateReloaded
     */
    public boolean isCrateGui(Inventory top) {
        return top != null && (top.getHolder() instanceof BaseHolder || inventoryManager.isManaged(top));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (isCrateGui(event.getView().getTopInventory())) {
            event.setCancelled(true);
            resync(event.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (isCrateGui(event.getView().getTopInventory())) {
            event.setCancelled(true);
            resync(event.getWhoClicked());
        }
    }

    private void resync(HumanEntity entity) {
        if (!(entity instanceof Player) || !plugin.isEnabled()) {
            return;
        }
        Player player = (Player) entity;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
    }
}
