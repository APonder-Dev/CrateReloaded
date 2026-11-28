package com.hazebyte.crate.cratereloaded.claim;

import com.hazebyte.crate.api.claim.Claim;
import com.hazebyte.crate.api.claim.ClaimRegistrar;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.cratereloaded.claim.storage.yaml.YamlClaimStorage;
import com.hazebyte.crate.cratereloaded.component.PluginSettingComponent;
import com.hazebyte.crate.cratereloaded.crate.OpeningLedger;
import com.hazebyte.crate.cratereloaded.menu.Size;
import com.hazebyte.crate.cratereloaded.menu.pages.ClaimPage;
import com.hazebyte.crate.cratereloaded.util.RewardFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ClaimManager implements ClaimRegistrar, OpeningLedger.PendingClaimStore {

    private final JavaPlugin plugin;
    private final YamlClaimStorage claimStorage;
    private final ClaimExecutor claimExecutor;
    private final PluginSettingComponent settings;

    public ClaimManager(
            @NonNull JavaPlugin plugin,
            @NonNull ClaimExecutor claimExecutor,
            @NonNull PluginSettingComponent settings) {
        this.plugin = plugin;
        this.claimStorage = new YamlClaimStorage(plugin, claimExecutor);
        this.claimExecutor = claimExecutor;
        this.settings = settings;

        // Claim files used to stay parsed in memory for the whole uptime once read. Drop a player's file when they
        // leave, and sweep files read for offline players (offline gives, admins viewing claims) every five minutes.
        plugin.getServer().getPluginManager().registerEvents(new UnloadOnQuit(), plugin);
        plugin.getServer()
                .getScheduler()
                .runTaskTimer(plugin, this::unloadOfflineClaimFiles, UNLOAD_PERIOD, UNLOAD_PERIOD);
    }

    private static final long UNLOAD_PERIOD = 20L * 60 * 5;

    private final class UnloadOnQuit implements org.bukkit.event.Listener {
        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
        public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
            claimStorage.unload(event.getPlayer().getUniqueId());
        }
    }

    /** Main thread: takes the online players, then unloads everyone else's claim file on the claim thread. */
    public CompletableFuture<Integer> unloadOfflineClaimFiles() {
        java.util.Set<UUID> online =
                Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toSet());
        return claimStorage.unloadAllExcept(online);
    }

    public CompletableFuture<Claim> addClaim(@NonNull Claim claim) {
        plugin.getLogger().finer("Claim: add " + claim.getOwner().getUniqueId());
        return claimStorage.saveClaim(claim).thenApply(v -> claim);
    }

    @Override
    public CompletableFuture<Claim> addClaim(@NonNull OfflinePlayer player, List<Reward> rewards) {
        CrateClaim claim = CrateClaim.builder()
                .owner(player)
                .rewards(rewards)
                .executor(claimExecutor)
                .build();
        return addClaim(claim);
    }

    @Override
    public CompletableFuture<Claim> addClaim(@NonNull OfflinePlayer player, Crate crate, int amount) {
        Reward reward = RewardFactory.createReward(crate, player, amount);
        return addClaim(player, Collections.singletonList(reward));
    }

    @Override
    public CompletableFuture<Void> removeClaim(@NonNull Claim claim) {
        return claimStorage.deleteClaim(claim);
    }

    /**
     * Waits for queued claim writes to reach disk and stops the storage thread. Call once, on disable.
     *
     * @return whether everything was written in time
     */
    public boolean flush() {
        return claimStorage.close(10);
    }

    @Override
    public CompletableFuture<?> save(Claim claim) {
        return addClaim(claim);
    }

    @Override
    public CompletableFuture<?> delete(Claim claim) {
        return removeClaim(claim);
    }

    @Override
    public CompletableFuture<Optional<Claim>> getClaim(@NonNull OfflinePlayer player, @NonNull UUID uuid) {
        return getClaims(player)
                .thenApply(claims -> claims.stream()
                        .filter(claim -> uuid.equals(claim.getId()))
                        .findFirst());
    }

    @Override
    public CompletableFuture<Collection<Claim>> getClaim(@NonNull OfflinePlayer player, @NonNull long timestamp) {
        return getClaims(player)
                .thenApply(claims -> claims.stream()
                        .filter(claim -> claim.getTimestamp() == timestamp)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Collection<Claim>> getClaims(@NonNull OfflinePlayer player) {
        return claimStorage.getClaims(player);
    }

    @Override
    public void openInventory(@NonNull Player player) {
        plugin.getLogger().finer("Claim: open " + player.getUniqueId());
        spoofInventory(player, player);
    }

    @Override
    public void spoofInventory(@NonNull OfflinePlayer player, @NonNull Player viewer) {
        getClaims(player)
                .thenAccept(claims -> {
                    plugin.getLogger().finer("Claim: spoof " + player.getUniqueId());
                    boolean buttonsEnabled = settings.isMenuInteractionEnabled();

                    // Calculate max slots: 54 max, minus 9 if buttons enabled
                    int maxSlots = buttonsEnabled ? 45 : 54;
                    // Cap claims to max displayable slots, pagination handles overflow
                    int displaySlots = Math.min(claims.size(), maxSlots);
                    // Add button row back if enabled for size calculation
                    int totalSlots = buttonsEnabled ? displaySlots + 9 : displaySlots;

                    Size size = Size.fit(totalSlots);
                    // Build the page on the main thread: its buttons format items and placeholders (Bukkit API,
                    // PlaceholderAPI), which is not safe from the claim storage thread.
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (viewer.isOnline()) {
                            new ClaimPage(claims, size, settings).open(viewer);
                        }
                    });
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to spoof claim inventory: " + throwable.getMessage());
                    if (throwable.getCause() != null) {
                        plugin.getLogger()
                                .severe("Caused by: " + throwable.getCause().getMessage());
                    }
                    return null;
                });
    }
}
