package com.hazebyte.crate.cratereloaded.component.impl;

import com.hazebyte.crate.api.CrateAPI;
import com.hazebyte.crate.api.claim.Claim;
import com.hazebyte.crate.api.claim.ClaimRegistrar;
import com.hazebyte.crate.api.util.Messenger;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.component.ClaimCrateComponent;
import com.hazebyte.crate.cratereloaded.component.PluginSettingComponent;
import com.hazebyte.crate.cratereloaded.util.PlayerUtil;
import com.hazebyte.crate.cratereloaded.util.format.CustomFormat;
import com.hazebyte.crate.exception.ExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ClaimCrateComponentImpl implements ClaimCrateComponent {

    private final CorePlugin plugin;
    private final ExceptionHandler exceptionHandler;
    private final PluginSettingComponent settings;

    @Inject
    public ClaimCrateComponentImpl(
            CorePlugin plugin, ExceptionHandler exceptionHandler, PluginSettingComponent settings) {
        this.plugin = plugin;
        this.exceptionHandler = exceptionHandler;
        this.settings = settings;
    }

    @Override
    public void openClaim(Player player) {
        ClaimRegistrar claimManager = plugin.getClaimRegistrar();
        claimManager
                .getClaims(player)
                .thenAccept(claims -> {
                    if (claims.size() > 0) {
                        claimManager.openInventory(player);
                    } else {
                        String message = CrateAPI.getMessage("core.claim_none");
                        Messenger.tell(player, CustomFormat.format(message));
                    }
                })
                .exceptionally(throwable -> {
                    exceptionHandler.handle((Exception) throwable);
                    Messenger.tell(player, "&cFailed to load your claims. Please contact an administrator.");
                    return null;
                });
    }

    @Override
    public void claimAllRewards(Player player) {
        ClaimRegistrar manager = plugin.getClaimRegistrar();
        manager.getClaims(player)
                // Read on the claim thread, redeem on the main thread (inventory and reward code is Bukkit API).
                .thenAccept(claims -> Bukkit.getScheduler().runTask(plugin, () -> redeemAll(player, manager, claims)))
                .exceptionally(throwable -> {
                    exceptionHandler.handle((Exception) throwable);
                    Messenger.tell(player, "&cFailed to load your claims. Please try again later.");
                    return null;
                });
    }

    /**
     * Pays claims one by one and deletes each only once it was paid. Deleting first lost the reward whenever the
     * payout was refused (inventory filled up part-way, or the claim belonged to an animation still running).
     */
    private void redeemAll(Player player, ClaimRegistrar manager, Collection<Claim> claims) {
        if (!player.isOnline()) {
            return;
        }
        List<Claim> redeemable = new ArrayList<>();
        for (Claim claim : claims) {
            if (plugin.getOpeningLedger() == null || !plugin.getOpeningLedger().isPending(claim.getId())) {
                redeemable.add(claim);
            }
        }
        if (redeemable.isEmpty()) {
            Messenger.tell(player, CustomFormat.format(CrateAPI.getMessage("core.claim_none")));
            return;
        }

        int limit = Math.min(redeemable.size(), Math.min(settings.getClaimLimit(), PlayerUtil.getSlotsLeft(player)));
        for (int i = 0; i < limit; i++) {
            Claim claim = redeemable.get(i);
            if (!claim.execute()) {
                break; // e.g. the inventory is full now; the rest stay claimable
            }
            try {
                manager.removeClaim(claim).exceptionally(throwable -> {
                    plugin.getLogger()
                            .log(
                                    Level.SEVERE,
                                    String.format("Failed to remove claim for player %s", player.getName()),
                                    throwable);
                    return null;
                });
            } catch (Exception e) {
                exceptionHandler.handle(e);
            }
        }
    }
}
