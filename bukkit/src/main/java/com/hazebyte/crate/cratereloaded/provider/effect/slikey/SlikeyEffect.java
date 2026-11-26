package com.hazebyte.crate.cratereloaded.provider.effect.slikey;

import com.hazebyte.crate.cratereloaded.CorePlugin;
import com.hazebyte.crate.cratereloaded.provider.effect.EffectWrapper;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.SoundEffect;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * A SlikeyEffect represents one unit of effect. e.g. There is an instance of SlikeyEffect for every
 * open and persistent effect that is run. In other words, everytime a crate is opened, a new
 * SlikeyEffect is created.
 */
public class SlikeyEffect implements EffectWrapper<Effect> {

    private Effect effect;

    public SlikeyEffect(Effect effect) {
        Objects.requireNonNull(effect);
        this.effect = effect;
    }

    @Override
    public void start() {
        effect.start();
    }

    @Override
    public void stop() {
        effect.cancel();
    }

    @Override
    public void setLocation(Location location) {
        effect.setLocation(location);
    }

    @Override
    public Optional<Location> getLocation() {
        return Optional.of(effect.getLocation());
    }

    @Override
    public Optional<Effect> getEffect() {
        return Optional.of(effect);
    }

    @Override
    public void setTargetPlayer(Player player) {
        effect.setTargetEntity(player);
        // setTargetEntity only makes the effect follow the player; it never limited who receives it, so the
        // "play only to the opener" branch in SoundEffect could not run and everyone nearby heard every crate
        // (#597). With crate.private-sounds on, sound effects go to the opener alone. Particles are unchanged.
        if (effect instanceof SoundEffect) {
            CorePlugin plugin = CorePlugin.getPlugin();
            boolean privateSounds = plugin != null
                    && plugin.getSettings() != null
                    && plugin.getSettings().isPrivateCrateSounds();
            effect.setTargetPlayer(privateSounds ? player : null);
        }
    }

    @Override
    public Player getTargetPlayer() {
        return effect.getTargetPlayer();
    }

    @Override
    public boolean isPersistent() {
        return effect.type == EffectType.REPEATING && effect.iterations == -1;
    }
}
