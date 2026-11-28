package com.hazebyte.crate.cratereloaded.provider;

import com.hazebyte.crate.cratereloaded.provider.holographic.HologramProvider;
import com.hazebyte.crate.cratereloaded.provider.holographic.cmi.CMIProvider;
import com.hazebyte.crate.cratereloaded.provider.holographic.dh.DecentHologramProvider;
import com.hazebyte.crate.cratereloaded.provider.holographic.none.NoHologramProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class HologramProviderSelector extends ProviderSelector<HologramProvider> {

    public static final String CMI = "CMI";
    public static final String DECENT_HOLOGRAM = "DecentHolograms";
    public static final String FANCY_HOLOGRAMS = "FancyHolograms";

    /** Only compiled into the Java 21+ builds; see FancyHologramProvider. */
    private static final String FANCY_PROVIDER_CLASS =
            "com.hazebyte.crate.cratereloaded.provider.holographic.fancy.FancyHologramProvider";

    public static final String NONE = "none";

    /** Tried in this order when the configured hologram plugin is not available. */
    public static final java.util.List<String> FALLBACK_ORDER =
            java.util.Collections.unmodifiableList(java.util.Arrays.asList(DECENT_HOLOGRAM, FANCY_HOLOGRAMS, CMI));

    public HologramProviderSelector(JavaPlugin plugin) {
        super(plugin);
        addProvider(CMI, CMIProvider.class, true);
        addProvider(DECENT_HOLOGRAM, DecentHologramProvider.class, true);
        try {
            addProvider(FANCY_HOLOGRAMS, Class.forName(FANCY_PROVIDER_CLASS).asSubclass(HologramProvider.class), true);
        } catch (ClassNotFoundException ignored) {
            // The 1.20 (Java 17) build has no FancyHolograms support: the FancyHolograms API needs Java 21.
        }
        addProvider(NONE, NoHologramProvider.class, false);
    }

    @Override
    public HologramProvider getNilProvider() {
        return new NoHologramProvider();
    }
}
