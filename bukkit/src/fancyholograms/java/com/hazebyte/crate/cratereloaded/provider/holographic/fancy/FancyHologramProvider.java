package com.hazebyte.crate.cratereloaded.provider.holographic.fancy;

import com.hazebyte.crate.cratereloaded.provider.holographic.HologramProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Location;

/**
 * Holograms through <a href="https://modrinth.com/plugin/fancyholograms">FancyHolograms</a>.
 *
 * <p>Compiled only into the Java 21+ builds (mc121, mc26): the FancyHolograms API is Java 21 bytecode, so the
 * Java 17 build (mc120) leaves this class out and {@code HologramProviderSelector} skips it.
 */
public class FancyHologramProvider implements HologramProvider<FancyHologram> {

    private static final String DEPENDENCY = "FancyHolograms";

    private final List<FancyHologram> hologramCache = new ArrayList<>();

    public FancyHologramProvider() {}

    @Override
    public Optional<FancyHologram> createHologram(Location location) {
        FancyHologram hologram = new FancyHologram(location);
        hologramCache.add(hologram);
        return Optional.of(hologram);
    }

    @Override
    public void removeHologram(FancyHologram hologram) {
        hologram.delete();
        hologramCache.remove(hologram);
    }

    @Override
    public void removeAll() {
        hologramCache.forEach(FancyHologram::delete);
        hologramCache.clear();
    }

    @Override
    public Optional<Collection<FancyHologram>> getHolograms() {
        return Optional.of(hologramCache.stream().filter(h -> !h.isDeleted()).collect(Collectors.toList()));
    }

    @Override
    public Optional<Collection<FancyHologram>> getHolograms(Location location) {
        return Optional.of(hologramCache.stream()
                .filter(h -> !h.isDeleted())
                .filter(h -> h.getWorld().equals(location.getWorld()))
                .filter(h -> h.getLocation().distance(location) < 1)
                .collect(Collectors.toList()));
    }

    @Override
    public String getName() {
        return DEPENDENCY;
    }
}
