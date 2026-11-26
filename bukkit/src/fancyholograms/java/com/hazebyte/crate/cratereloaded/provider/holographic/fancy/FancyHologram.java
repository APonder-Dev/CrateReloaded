package com.hazebyte.crate.cratereloaded.provider.holographic.fancy;

import com.hazebyte.crate.cratereloaded.provider.holographic.HologramWrapper;
import com.hazebyte.crate.cratereloaded.provider.holographic.VisibilityAPI;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * A crate hologram backed by a FancyHolograms text hologram (a text display entity).
 *
 * <p>The hologram is not persistent: FancyHolograms never saves it, and CrateReloaded recreates its holograms on
 * start. Lines are kept here and pushed as a whole with {@link TextHologramData#setText(List)}. FancyHolograms
 * renders MiniMessage, so legacy colour codes are converted first.
 */
public class FancyHologram implements HologramWrapper {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final String name = "cratereloaded_" + UUID.randomUUID();
    private final List<String> lines = new ArrayList<>();
    private final TextHologramData data;
    private final Hologram hologram;
    private final long created = System.currentTimeMillis();
    private boolean deleted;

    public FancyHologram(Location location) {
        data = new TextHologramData(name, location);
        data.setPersistent(false);
        data.setText(new ArrayList<>());
        HologramManager manager = manager();
        hologram = manager.create(data);
        manager.addHologram(hologram);
    }

    private static HologramManager manager() {
        return FancyHologramsPlugin.get().getHologramManager();
    }

    private static String toMiniMessage(String legacy) {
        String section = legacy.replace('&', '§');
        return MiniMessage.miniMessage().serialize(LEGACY.deserialize(section));
    }

    private static String itemText(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name().toLowerCase().replace('_', ' ');
    }

    private void push() {
        List<String> text = new ArrayList<>(lines.size());
        for (String line : lines) {
            text.add(toMiniMessage(line));
        }
        data.setText(text);
        hologram.forceUpdate();
    }

    @Override
    public void addItemLine(ItemStack item) {
        // Text holograms can't show items: show the item's name instead.
        addTextLine(itemText(item));
    }

    @Override
    public void addTextLine(String text) {
        lines.add(text);
        push();
    }

    @Override
    public void clearLines() {
        lines.clear();
        push();
    }

    @Override
    public void delete() {
        if (!deleted) {
            deleted = true;
            manager().removeHologram(hologram);
        }
    }

    @Override
    public long getCreationTimestamp() {
        return created;
    }

    @Override
    public double getHeight() {
        return 0;
    }

    @Override
    public void getLine(int index) {
        // Not used by CrateReloaded.
    }

    @Override
    public Location getLocation() {
        return data.getLocation();
    }

    @Override
    public VisibilityAPI getVisibilityManager() {
        return null;
    }

    @Override
    public World getWorld() {
        return getLocation().getWorld();
    }

    @Override
    public double getX() {
        return getLocation().getX();
    }

    @Override
    public double getY() {
        return getLocation().getY();
    }

    @Override
    public double getZ() {
        return getLocation().getZ();
    }

    @Override
    public void insertItemLine(int index, ItemStack item) {
        insertTextLine(index, itemText(item));
    }

    @Override
    public void insertTextLine(int index, String text) {
        lines.add(Math.max(0, Math.min(index, lines.size())), text);
        push();
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            push();
        }
    }

    @Override
    public int size() {
        return lines.size();
    }

    @Override
    public void teleport(Location location) {
        data.setLocation(location);
        hologram.forceUpdate();
    }

    @Override
    public void teleport(World world, double x, double y, double z) {
        teleport(new Location(world, x, y, z));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FancyHologram && Objects.equals(name, ((FancyHologram) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
