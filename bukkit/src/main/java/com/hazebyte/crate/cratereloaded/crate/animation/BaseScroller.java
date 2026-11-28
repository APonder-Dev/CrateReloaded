package com.hazebyte.crate.cratereloaded.crate.animation;

import com.hazebyte.crate.api.CrateAPI;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.api.effect.Category;
import com.hazebyte.crate.api.event.ClaimGiveEvent;
import com.hazebyte.crate.api.util.Messenger;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.claim.CrateClaim;
import com.hazebyte.crate.cratereloaded.component.PluginSettingComponent;
import com.hazebyte.crate.cratereloaded.menu.Size;
import com.hazebyte.crate.cratereloaded.util.format.CustomFormat;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class BaseScroller extends Animation {

    protected AnimationSpeed speed;
    protected final PluginSettingComponent settings;

    public BaseScroller(Crate crate, PluginSettingComponent settings) {
        super(crate, settings);
        this.settings = settings;
    }

    public BaseScroller(Crate crate, int length, PluginSettingComponent settings) {
        super(crate, length, settings);
        this.settings = settings;
    }

    public BaseScroller(Crate crate, int length, Size size, PluginSettingComponent settings) {
        super(crate, length, size, settings);
        this.settings = settings;
    }

    @Override
    public void setDefault() {
        speed = new AnimationSpeed(this.length);
        speed.add(0.0125, 6);
        speed.add(0.0125, 5);
        speed.add(0.0125, 4);
        speed.add(0.0125, 3);
        speed.add(0.40, 2);
        speed.add(0.0125, 3);
        speed.add(0.0125, 4);
        speed.add(0.0125, 5);
        speed.add(0.0125, 6);
        speed.add(0.20, 10);
        speed.add(0.30, 20);

        this.numberOfPrizes = speed.getNumberOfPrizes();
        String debug = String.format("Crate: %s - %s", crate.getCrateName(), numberOfPrizes);
        CorePlugin.getPlugin().getLogger().finer(debug);
    }

    public Inventory openCrate(Player player, Location location) {
        List<Reward> rewards = getRewards(player);
        // always:() rewards are owed on every opening: animated crates used to never give them (only the
        // reward the animation landed on), while instant and supply crates did.
        List<Reward> always = crate.getConstantRewards().stream()
                .filter(reward -> !reward.hasPermission(player))
                .collect(java.util.stream.Collectors.toList());
        AnimationHolder holder = new AnimationHolder(this, player, rewards, crate.getConstantRewards());
        Inventory inventory = Bukkit.createInventory(holder, size.getSize(), crate.getDisplayName());

        CorePlugin.getPlugin().getLogger().finer(String.format("Generated %d for animation. ", rewards.size()));

        // Persist the reward the animation will land on before it starts. The fallback used to be
        // rewards.get(0), a different roll: a player who saw a poor result coming (or landed) could disconnect
        // before delivery and be paid that other reward instead.
        AnimationTask task = task(inventory, player, rewards, location);
        beginOpening(player, Collections.singletonList(task.landingReward()), always);

        crate.runEffect(location, Category.OPEN, player);

        task.run();
        player.openInventory(inventory);
        return inventory;
    }

    @Override
    public void onEnd(Player player, Location location, Reward reward, Inventory inventory) {
        if (this.ending == null) {
            this.onReward(player, location, reward);
            return;
        }

        this.ending.startClosing(player, location, reward, inventory);
    }

    @Override
    public void onEnd(Player player, Location location, List<Reward> reward, Inventory inventory) {
        if (this.ending == null) {
            this.onReward(player, location, reward);
            return;
        }
        this.ending.startClosing(player, location, reward, inventory);
    }

    @Override
    public void onDisable(List<Reward> rewards, List<Reward> constantRewards, Player player) {
        // Settled by the ledger: pays the persisted prize once. Previously, with claims enabled, this stored
        // the whole pre-generated reward list (numberOfPrizes + 1 rewards) as claims for a single key.
        abortOpening(player);
    }

    private void handleClaims(Player player, List<Reward> rewards) {
        CorePlugin.getPlugin().getClaimRegistrar().addClaim(player, rewards);

        if (player.isOnline()) {
            CrateClaim claim = CrateClaim.builder()
                    .owner(player)
                    .rewards(rewards)
                    .executor(CorePlugin.getJavaPluginComponent().getClaimExecutor())
                    .build();
            ClaimGiveEvent claimGiveEvent = new ClaimGiveEvent(claim);
            Bukkit.getPluginManager().callEvent(claimGiveEvent);

            if (claimGiveEvent.isCancelled()) {
                return;
            }

            String format = CrateAPI.getMessage("core.claim_animation");
            String message = CustomFormat.format(format, player);

            Messenger.tell(player, message);
        }
    }
}
