package com.hazebyte.crate.cratereloaded.util.format;

import com.hazebyte.crate.api.CrateAPI;
import com.hazebyte.crate.api.ServerVersion;
import com.hazebyte.crate.api.util.ColorUtil;
import com.hazebyte.crate.api.util.Messenger;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import java.util.Calendar;
import java.util.Map;
import org.bukkit.ChatColor;

public class DefaultFormat extends Format {

    public DefaultFormat(String message) {
        super(message);
    }

    @Override
    public String format(Object object) {
        return format();
    }

    public String format() {
        if (message == null || message.isEmpty()) return "";
        // Longest key first, so "{rank}" is replaced before a bare "rank" could eat its middle; HashMap order
        // made the result depend on iteration order.
        java.util.List<Map.Entry<String, String>> variables = new java.util.ArrayList<>(
                CorePlugin.getPlugin().getSettings().getCustomVariables().entrySet());
        variables.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (Map.Entry<String, String> entry : variables) {
            if (entry.getValue() != null) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        message = message.replace("{prefix}", Messenger.getPrefix())
                .replace("{p}", Messenger.getPrefix())
                .replace(
                        "{day}",
                        String.valueOf(CorePlugin.getPlugin()
                                .getSettings()
                                .getCalendar()
                                .get(Calendar.DAY_OF_MONTH)))
                .replace(
                        "{days}",
                        String.valueOf(CorePlugin.getPlugin()
                                .getSettings()
                                .getCalendar()
                                .get(Calendar.DAY_OF_YEAR)))
                .replace(
                        "{month}",
                        // Calendar.MONTH is 0-based: January printed as 0.
                        String.valueOf(CorePlugin.getPlugin()
                                        .getSettings()
                                        .getCalendar()
                                        .get(Calendar.MONTH)
                                + 1))
                .replace(
                        "{year}",
                        String.valueOf(CorePlugin.getPlugin()
                                .getSettings()
                                .getCalendar()
                                .get(Calendar.YEAR)));
        message = ChatColor.translateAlternateColorCodes('&', message);

        if (ServerVersion.getVersion().gte(ServerVersion.v1_16_R1)) {
            message = ColorUtil.of(message);
        }

        if (CrateAPI.getInstance().isReady()) {
            String list = CrateAPI.getCrateRegistrar().getCrateString();
            message = message.replace("{crates}", list);
        }
        return message;
    }
}
