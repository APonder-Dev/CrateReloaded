package com.hazebyte.crate.cratereloaded.claim;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.crate.reward.Reward;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * #600: claims written by 2.3.8 embed raw item NBT in the display item. Those reward strings must still parse
 * into a usable reward, so the key can be claimed.
 */
class Issue600RewardParsingTest extends BukkitTest {

    /** "Old key" from the report, as YAML loads it (folded lines joined by spaces). */
    static final String OLD = "chance:(0.0), display:(TRIPWIRE_HOOK 2 name:&bStemmenøkkel "
            + "lore:&eKan_brukes_til_å_åpne|&a&oStemmekasse_&ei_spawn! arrow_infinite:10 hide:true), "
            + "cmd:(/crate give to solvind70 stemmenøkkel 2)";

    /** "New key" from the report: same reward, plus the NBT 2.3.8 appended to the display item. */
    static final String NEW = "chance:(0.0), display:(TRIPWIRE_HOOK 2 name:&bStemmenøkkel "
            + "lore:&eKan_brukes_til_å_åpne|&a&oStemmekasse_&ei_spawn! arrow_infinite:10 hide:true "
            + "{Enchantments:[{id:\"minecraft:infinity\",lvl:10s}],HideFlags:1,display:{Lore:['{\"text\":\"\","
            + "\"extra\":[{\"text\":\"Kan brukes til å åpne\",\"obfuscated\":false,\"italic\":false,"
            + "\"underlined\":false,\"strikethrough\":false,\"color\":\"yellow\",\"bold\":false}]}',"
            + "'{\"text\":\"\",\"extra\":[{\"text\":\"Stemmekasse \",\"obfuscated\":false,\"italic\":true,"
            + "\"underlined\":false,\"strikethrough\":false,\"color\":\"green\",\"bold\":false},{\"text\":\"i "
            + "spawn!\",\"italic\":false,\"color\":\"yellow\"}]}'],Name:'{\"text\":\"\",\"extra\":[{\"text\":"
            + "\"Stemmenøkkel\",\"obfuscated\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,"
            + "\"color\":\"aqua\",\"bold\":false}]}'}}), cmd:(/crate give to Karius_79 stemmenøkkel 2)";

    @Test
    void appendedNbtIsStripped_butSingleTokenNbtIsKept() {
        Assertions.assertEquals(
                "DIRT 1 name:&aKey hide:true",
                com.hazebyte.crate.cratereloaded.util.item.ItemParser.stripAppendedNbt(
                        "DIRT 1 name:&aKey hide:true {display:{Name:'{\"text\":\"a b\"}'}}"));
        String singleToken = "DIRT 1 {CustomModelData:5}";
        Assertions.assertEquals(
                singleToken, com.hazebyte.crate.cratereloaded.util.item.ItemParser.stripAppendedNbt(singleToken));
        Assertions.assertEquals(
                "DIRT 1 name:&aKey",
                com.hazebyte.crate.cratereloaded.util.item.ItemParser.stripAppendedNbt("DIRT 1 name:&aKey"));
    }

    @Test
    void oldFormatKeyClaim_parses() {
        Reward reward = plugin.getCrateRegistrar().createReward(OLD);
        Assertions.assertNotNull(reward);
        Assertions.assertFalse(reward.getCommands().isEmpty());
    }

    @Test
    void keyClaimWithEmbeddedNbt_parses() {
        Reward reward = plugin.getCrateRegistrar().createReward(NEW);
        Assertions.assertNotNull(reward, "2.3.8-format claim must still parse (#600)");
        Assertions.assertEquals(
                "/crate give to Karius_79 stemmenøkkel 2", reward.getCommands().get(0), "the key command survives");
    }
}
