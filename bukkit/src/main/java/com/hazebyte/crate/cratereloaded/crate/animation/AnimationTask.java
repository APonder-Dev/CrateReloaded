package com.hazebyte.crate.cratereloaded.crate.animation;

import com.hazebyte.crate.api.crate.reward.Reward;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class AnimationTask extends BukkitRunnable {
    protected BaseScroller parent;
    protected Inventory inventory;
    protected Player player;
    protected int speed;
    protected int totalTicks;
    protected int timeLapsed;
    protected int iterations;
    protected List<Reward> rewards;
    protected Location location;

    public AnimationTask(BaseScroller parent, Inventory inventory, Player player, int length, Location location) {
        this.parent = parent;
        this.inventory = inventory;
        this.player = player;
        this.speed = 2;
        this.totalTicks = length;
        this.timeLapsed = 0;
        this.iterations = 0;
        this.location = location;
    }

    public AnimationTask(AnimationTask previous) {
        this.parent = previous.parent;
        this.inventory = previous.inventory;
        this.player = previous.player;
        this.speed = previous.speed;
        this.totalTicks = previous.totalTicks;
        this.timeLapsed = previous.timeLapsed;
        this.iterations = previous.iterations;
        this.rewards = previous.rewards;
        this.location = previous.location;
    }

    @Override
    public abstract void run();

    public abstract void update(Player player, Inventory inventory, List<Reward> rewards);

    public void sync() {
        this.timeLapsed += speed;
        // No player.updateInventory() here: it resent the whole window (crate and player inventory) every frame.
        // Slot changes already reach the client, and CrateGuiGuardListener resyncs after any blocked click.

        //        Messenger.info(timeLapsed + "/" + totalTicks + " " + iterations);
        int index = this.parent.speed.getIndex(this.timeLapsed);
        if (index < 0) {
            this.speed = -1;
        } else {
            this.speed = this.parent.speed.getSpeed(index);
        }
    }

    public boolean shouldStop(int numberOfPrizes) {
        if (stopsAt(timeLapsed, iterations, numberOfPrizes)) {
            return true;
        }
        this.iterations++;
        return false;
    }

    /** The stop condition of {@link #shouldStop}, without side effects. */
    protected boolean stopsAt(int timeLapsed, int iterations, int numberOfPrizes) {
        return timeLapsed >= totalTicks || iterations >= numberOfPrizes;
    }

    /**
     * Replays {@link #sync()} and {@link #shouldStop} from the current state without touching the inventory.
     * Tasks are rescheduled by tick count, so this is exactly the {@code iterations} value the animation will
     * stop at, whatever the server's TPS.
     *
     * @return the iterations at the stop, or -1 if the schedule never stops (a broken speed table)
     */
    protected int predictStopIterations() {
        int time = timeLapsed;
        int step = speed;
        int count = iterations;
        for (int guard = 0; guard < 100_000; guard++) {
            time += step;
            int index = parent.speed.getIndex(time);
            step = index < 0 ? -1 : parent.speed.getSpeed(index);
            if (stopsAt(time, count, parent.numberOfPrizes)) {
                return count;
            }
            count++;
        }
        return -1;
    }

    /**
     * The reward this animation will land on. It is persisted as the fallback prize when the opening starts,
     * so leaving mid-animation pays exactly what the animation shows, never a different roll.
     *
     * @return the reward the animation lands on
     */
    public Reward landingReward() {
        return rewards.get(0);
    }

    /** {@code rewards.get(index)}, or the first reward if the index is out of range. */
    protected Reward rewardAt(int index) {
        return index >= 0 && index < rewards.size() ? rewards.get(index) : rewards.get(0);
    }
}
