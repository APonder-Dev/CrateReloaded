package com.hazebyte.crate.cratereloaded.component.impl;

import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.component.GenerateCratePrizeComponent;
import com.hazebyte.crate.cratereloaded.component.RewardServiceComponent;
import com.hazebyte.crate.cratereloaded.crate.generator.rules.PermissionNotAllowedRule;
import com.hazebyte.crate.cratereloaded.crate.generator.rules.RewardHasChanceRule;
import com.hazebyte.crate.cratereloaded.crate.generator.rules.UniqueRewardRule;
import com.hazebyte.crate.cratereloaded.model.CrateV2;
import com.hazebyte.crate.cratereloaded.model.RewardV2;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import org.bukkit.entity.Player;

public class GenerateCratePrizeComponentImpl implements GenerateCratePrizeComponent {

    private final CorePlugin plugin;
    private final RewardServiceComponent generateRewardComponent;

    @Inject
    public GenerateCratePrizeComponentImpl(CorePlugin plugin, RewardServiceComponent generateRewardComponent) {
        this.plugin = plugin;
        this.generateRewardComponent = generateRewardComponent;
    }

    @Override
    public List<Reward> generateCratePrize(
            Crate crate, Player player, int amountOfPrizes, boolean overrideUniqueRule, boolean addConstantRewards) {
        List<Reward> winnings = new ArrayList<>();

        if (addConstantRewards) {
            winnings.addAll(crate.getConstantRewards().stream()
                    .filter(reward -> !reward.hasPermission(player))
                    .collect(Collectors.toList()));
        }

        int expectedNumberOfRewards = amountOfPrizes + winnings.size();

        while (winnings.size() < expectedNumberOfRewards) {
            List<Predicate<Reward>> rules = new ArrayList<>();
            rules.add(new PermissionNotAllowedRule(player));
            rules.add(new RewardHasChanceRule());
            if (!overrideUniqueRule) {
                rules.add(new UniqueRewardRule(winnings));
            }

            List<Reward> prizePool = generateRewardComponent.createPrizePool(crate.getRewards(), rules);

            if (prizePool.size() <= 0) {
                String errorMessage = String.format(
                        "Prize pool is empty for player [%s] and crate [%s].", player.getName(), crate.getCrateName());
                plugin.getLogger().warning(errorMessage);
                break;
            }

            Reward reward = generateRewardComponent.generatePrize(prizePool);
            winnings.add(reward);
        }

        return postParsing(winnings);
    }

    @Override
    public RewardV2 generateRewardForAnimation(Player player, CrateV2 crateV2) {
        List<Predicate<RewardV2>> rules = new ArrayList<>();
        rules.add(reward -> isAllowed(reward, player));
        rules.add(reward -> reward.getChance() > 0);
        val rewardPool = generateRewardComponent.createPrizePoolV2(crateV2.getRewards(), rules);
        val rewardV2 = generateRewardComponent.generatePrizeV2(rewardPool);
        return rewardV2;
    }

    /**
     * V2 reward permission rules, consistent with the legacy {@code permission:(node)} tag (which the mapper turns
     * into {@code exclusivePermissions}):
     *
     * <ul>
     *   <li>{@code exclusive-permissions}: the reward is <em>excluded</em> for a player who has any of them;
     *   <li>{@code inclusive-permissions}: the reward is only available to a player who has all of them.
     * </ul>
     *
     * <p>This used to require every exclusive permission and ignore the inclusive list, so a legacy
     * {@code permission:} reward was inverted when it ran through a V2 animation.
     */
    static boolean isAllowed(RewardV2 reward, Player player) {
        boolean excluded = reward.getExclusivePermissions().stream().anyMatch(player::hasPermission);
        boolean included = reward.getInclusivePermissions().stream().allMatch(player::hasPermission);
        return !excluded && included;
    }

    protected List<Reward> postParsing(List<Reward> rewards) {
        List<Reward> list = new ArrayList<>();
        for (Reward reward : rewards) {
            Reward clone = postParsing(reward);
            list.add(clone);
        }
        return list;
    }

    protected Reward postParsing(Reward reward) {
        if (reward.hasPostParsing()) {
            return reward.copy();
        }
        return reward;
    }
}
