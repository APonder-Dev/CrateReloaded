package com.hazebyte.crate.cratereloaded.util.format;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.cratereloaded.component.impl.FilePluginSettingComponentImpl;
import com.hazebyte.crate.cratereloaded.model.RewardImpl;
import com.hazebyte.crate.cratereloaded.util.item.ItemParser;
import java.lang.reflect.Field;
import java.util.Calendar;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** P1 formatting issues: #611 custom variables, #586 {player-name} in item:, #610 {date}, and {month}. */
class FormattingRegressionTest extends BukkitTest {

    private FileConfiguration config() throws Exception {
        FilePluginSettingComponentImpl settings = plugin.getSettings();
        Field field = FilePluginSettingComponentImpl.class.getDeclaredField("fileConfig");
        field.setAccessible(true);
        return (FileConfiguration) field.get(settings);
    }

    @BeforeEach
    void variables() throws Exception {
        FileConfiguration config = config();
        // As documented: custom-variables: '{sword_enchant}': 'sharpness:1 knockback:1'
        config.set("custom-variables.{sword_enchant}", "sharpness:1 knockback:1");
        config.set("custom-variables.{server}", "&6DrugRun");
        plugin.getSettings().clearCache();
    }

    @Test
    void customVariables_expandInItemStrings() {
        ItemStack sword = ItemParser.parse("DIAMOND_SWORD 1 name:{server}_Blade {sword_enchant}");
        Assertions.assertEquals(1, sword.getEnchantmentLevel(Enchantment.SHARPNESS), "variable with spaces expands");
        Assertions.assertEquals(1, sword.getEnchantmentLevel(Enchantment.KNOCKBACK));
        Assertions.assertEquals(
                "DrugRun Blade", ChatColor.stripColor(sword.getItemMeta().getDisplayName()), "variable in a name");
    }

    @Test
    void customVariableDefinedWithoutBraces_matchesBracedUse() throws Exception {
        config().set("custom-variables.rank", "VIP");
        plugin.getSettings().clearCache();
        Assertions.assertEquals("Rank: VIP", CustomFormat.format("Rank: {rank}"));
    }

    @Test
    void customVariables_expandInRewardDisplays() {
        RewardImpl reward = new RewardImpl("chance:(1), display:(DIRT 1 name:{server}_Prize)");
        Assertions.assertEquals(
                "DrugRun Prize", ChatColor.stripColor(reward.getDisplayItem().getItemMeta().getDisplayName()));
    }

    @Test
    void playerPlaceholders_areReplacedInGivenItems() {
        PlayerMock player = server.addPlayer("Alice");
        RewardImpl reward = new RewardImpl("chance:(1), item:(DIRT 1 name:{player}'s_dirt)");
        ItemStack given = reward.getItems(player).get(0);
        Assertions.assertEquals("Alice's dirt", ChatColor.stripColor(given.getItemMeta().getDisplayName()));
    }

    @Test
    void dateAndMonth_areFormatted() {
        String date = CustomFormat.format("claimed {date}", 2, 1711458318507L);
        Assertions.assertFalse(date.contains("{date}"), "a Long argument fills {date} (#610)");

        int month = plugin.getSettings().getCalendar().get(Calendar.MONTH) + 1;
        Assertions.assertEquals("m" + month, CustomFormat.format("m{month}"), "{month} is 1-12, not 0-11");
    }
}
