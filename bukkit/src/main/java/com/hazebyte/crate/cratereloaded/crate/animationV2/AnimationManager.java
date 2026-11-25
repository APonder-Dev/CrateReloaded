package com.hazebyte.crate.cratereloaded.crate.animationV2;

import static com.hazebyte.crate.cratereloaded.util.GeneralConstants.FIVE_MINUTES_TICKS;

import com.hazebyte.crate.api.CrateAPI;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.api.effect.Category;
import com.hazebyte.crate.api.event.ClaimGiveEvent;
import com.hazebyte.crate.api.util.Messenger;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.crate.OpeningLedger;
import com.hazebyte.crate.cratereloaded.claim.ClaimExecutor;
import com.hazebyte.crate.cratereloaded.claim.CrateClaim;
import com.hazebyte.crate.cratereloaded.crate.animationV2.events.AnimationCloseEvent;
import com.hazebyte.crate.cratereloaded.crate.animationV2.events.AnimationFrameChangeEvent;
import com.hazebyte.crate.cratereloaded.util.format.CustomFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AnimationManager {

    private final JavaPlugin plugin;
    private final CorePlugin corePlugin;
    private final ClaimExecutor claimExecutor;
    private final Map<UUID, AnimationExecution> ongoingAnimations;

    public AnimationManager(
            @NonNull JavaPlugin plugin, @NonNull CorePlugin corePlugin, @NonNull ClaimExecutor claimExecutor) {
        this.plugin = plugin;
        this.corePlugin = corePlugin;
        this.claimExecutor = claimExecutor;
        this.ongoingAnimations = new ConcurrentHashMap<>();

        plugin.getServer().getPluginManager().registerEvents(new AnimationListener(this), plugin);
        plugin.getServer()
                .getScheduler()
                .runTaskTimerAsynchronously(
                        plugin,
                        () -> {
                            new ArrayList<>(ongoingAnimations.keySet())
                                    .stream()
                                            .filter(uuid -> {
                                                Player player = Bukkit.getPlayer(uuid);
                                                return player == null
                                                        || !player.isOnline()
                                                        || ongoingAnimations
                                                                .get(uuid)
                                                                .isCancelled();
                                            })
                                            .forEach(ongoingAnimations::remove);
                        },
                        FIVE_MINUTES_TICKS,
                        FIVE_MINUTES_TICKS);
    }

    // False when the player is already animating, so the caller can reject and skip the key
    public boolean startAnimation(@NonNull Animation animation) {
        UUID playerId = animation.getPlayer().getUniqueId();
        if (ongoingAnimations.containsKey(playerId)) {
            AnimationExecution animationExecution = ongoingAnimations.get(playerId);
            if (animationExecution.isCancelled()) {
                ongoingAnimations.remove(playerId);
            } else {
                return false;
            }
        }

        // Persist the prize before the animation starts, so quitting, a disable or a crash can't lose it (#540).
        OpeningLedger ledger = corePlugin.getOpeningLedger();
        if (ledger != null) {
            ledger.begin(animation.getPlayer(), Collections.singletonList(winningReward(animation)));
        }

        AnimationExecution animationExecution = new AnimationExecution(animation);
        animationExecution.runTaskTimer(plugin, 0L, 1L);
        ongoingAnimations.put(playerId, animationExecution);
        return true;
    }

    public void onInventoryClose(@NonNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (!ongoingAnimations.containsKey(playerId)) {
            return;
        }

        AnimationExecution animationExecution = ongoingAnimations.get(playerId);
        if (animationExecution.isCancelled()) {
            ongoingAnimations.remove(playerId);
            return;
        }

        if (corePlugin.getSettings().allowsInventoryExiting()) {
            animationExecution.cancelAnimation(false);
        } else {
            // this needs to be a scheduler or else an infinite loop will happen.
            // this will close the existing inventory, reopen, close existing...
            Bukkit.getScheduler()
                    .runTaskLater(
                            corePlugin,
                            () -> corePlugin
                                    .getJavaPluginComponent()
                                    .getInventoryManager()
                                    .openInventory(animationExecution.getInventoryV2(), player),
                            1L);
        }
    }

    /**
     * Settles the opening once. A completed animation with the player still online pays the winning reward
     * as before. Anything else (exited early, or the player went offline, which the execution also reports
     * as "completed") goes through {@link OpeningLedger#abort}: delivered now if the player is online and
     * claims are off, otherwise kept as a claim. Previously an offline player got
     * {@code executeReward} on a disconnected entity, and the prize was lost.
     */
    public void onAnimationClose(@NonNull AnimationCloseEvent event) {
        Animation animation = event.getAnimation();
        Player player = animation.getPlayer();
        OpeningLedger ledger = corePlugin.getOpeningLedger();
        boolean finishedNormally = event.getIsCompleted() && player.isOnline();

        if (!finishedNormally) {
            if (ledger != null) {
                ledger.abort(player.getUniqueId());
            }
            return;
        }
        if (ledger != null && !ledger.complete(player.getUniqueId())) {
            return; // already settled
        }

        Reward reward = winningReward(animation);
        CorePlugin.getJavaPluginComponent()
                .getOpenCrateComponent()
                .executeCrateV2Message(player, animation.getCrateV2(), Collections.singletonList(reward));
        CorePlugin.getJavaPluginComponent().getOpenCrateComponent().executeReward(player, reward);
    }

    private static Reward winningReward(Animation animation) {
        return CorePlugin.REWARD_MAPPER.toImplementation(animation.getWinningRewards().get(0));
    }

    public void onFrameChange(@NonNull AnimationFrameChangeEvent event) {
        val crate = corePlugin
                .getCrateRegistrar()
                .getCrate(event.getAnimation().getCrateV2().getCrateName());
        val player = event.getAnimation().getPlayer();
        if (event.getCurrentFrameIndex() == event.getAnimation().getFrames().size()) {
            crate.runEffect(player.getLocation(), Category.END, player);
        } else {
            crate.runEffect(player.getLocation(), Category.ANIMATION, player);
        }
    }
}
