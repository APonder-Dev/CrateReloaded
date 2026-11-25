package com.hazebyte.crate.cratereloaded.util;

import static com.hazebyte.crate.test.ItemStackMockData.ONE_INVENTORY_OF_STONE;
import static com.hazebyte.crate.test.ItemStackMockData.ONE_INVENTORY_OF_STONE_PLUS_ONE_EXTRA;
import static com.hazebyte.crate.test.ItemStackMockData.ONE_STONE;

import com.hazebyte.crate.BukkitTest;
import com.hazebyte.crate.api.util.ItemBuilder;
import com.hazebyte.crate.test.InventoryMockData;
import com.hazebyte.crate.test.ItemStackMockData;
import com.hazebyte.crate.test.PlayerMockData;
import java.util.Optional;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

public class ItemCalculatorTest extends BukkitTest {

    private PlayerMock playerMock;

    @BeforeEach
    public void setup() {
        playerMock = server.addPlayer();
    }

    @ParameterizedTest
    @MethodSource("provideArgsToGetNumberOfSlotsRequired")
    public void getNumberOfSlotsRequired(ItemStack item, int expectedNumberOfSlots) {
        int slots = ItemCalculatorUtil.getNumberOfSlotsRequired(item);
        Assertions.assertEquals(expectedNumberOfSlots, slots);
    }

    private static Stream<Arguments> provideArgsToGetNumberOfSlotsRequired() {
        return Stream.of(
                Arguments.of(new ItemBuilder(Material.STONE, 1).asItemStack(), 1),
                Arguments.of(new ItemBuilder(Material.STONE, 63).asItemStack(), 1),
                Arguments.of(new ItemBuilder(Material.STONE, 64).asItemStack(), 1),
                Arguments.of(new ItemBuilder(Material.STONE, 65).asItemStack(), 2),
                Arguments.of(new ItemBuilder(Material.STONE, 128).asItemStack(), 2),
                Arguments.of(new ItemBuilder(Material.STONE, 64 * 20).asItemStack(), 20),
                Arguments.of(new ItemBuilder(Material.CAKE, 1).asItemStack(), 1),
                // #486: cake, totems and armour do not stack; ender pearls stack to 16.
                // (The old expectation of 1 slot for 64 cakes encoded the bug.)
                Arguments.of(new ItemBuilder(Material.CAKE, 64).asItemStack(), 64),
                Arguments.of(new ItemBuilder(Material.TOTEM_OF_UNDYING, 5).asItemStack(), 5),
                Arguments.of(new ItemBuilder(Material.DIAMOND_CHESTPLATE, 2).asItemStack(), 2),
                Arguments.of(new ItemBuilder(Material.ENDER_PEARL, 20).asItemStack(), 2));
    }

    @ParameterizedTest
    @MethodSource("validateLeftover_forPutItemsIntoInventory")
    public void validateLeftover_forPutItemsIntoInventory(
            ItemStack item, ItemStack expectedLeftover, PlayerMock playerMock) {
        Optional<ItemStack> leftover = ItemCalculatorUtil.putItemsIntoInventory(playerMock, item);

        Assertions.assertEquals(expectedLeftover != null, leftover.isPresent());
        if (expectedLeftover != null) {
            Assertions.assertEquals(expectedLeftover, leftover.get());
        }
    }

    private static Stream<Arguments> validateLeftover_forPutItemsIntoInventory() {
        return Stream.of(
                Arguments.of(ONE_STONE, null, server.addPlayer()),
                Arguments.of(ONE_INVENTORY_OF_STONE, null, server.addPlayer()),
                Arguments.of(ONE_INVENTORY_OF_STONE_PLUS_ONE_EXTRA, ONE_STONE, server.addPlayer()),
                Arguments.of(ONE_STONE, ONE_STONE, PlayerMockData.createPlayerWithFullInventory(server)));
    }

    @ParameterizedTest
    @MethodSource("validateInventory_forPutItemsIntoInventory")
    public void validateInventory_forPutItemsIntoInventory(
            ItemStack item, ItemStack[] expectedContents, PlayerMock playerMock) {
        ItemCalculatorUtil.putItemsIntoInventory(playerMock, item);
        ItemStack[] contents = playerMock.getInventory().getContents();
        Assertions.assertArrayEquals(expectedContents, contents);
    }

    private static Stream<Arguments> validateInventory_forPutItemsIntoInventory() {
        return Stream.of(
                Arguments.of(ONE_STONE, InventoryMockData.ONE_STONE_INVENTORY, server.addPlayer()),
                Arguments.of(ONE_INVENTORY_OF_STONE, InventoryMockData.FULL_STONE_INVENTORY, server.addPlayer()),
                Arguments.of(
                        ONE_INVENTORY_OF_STONE_PLUS_ONE_EXTRA,
                        InventoryMockData.FULL_STONE_INVENTORY,
                        server.addPlayer()),
                Arguments.of(
                        ItemStackMockData.ONE_MINECART, InventoryMockData.ONE_MINECART_INVENTORY, server.addPlayer())
                // mock tests do not seem to appear to allow stacking but spigot does.
                //        Arguments.of(ItemStackMockData.ONE_INVENTORY_OF_MINECART,
                // InventoryMockData.FULL_MINECART_INVENTORY, server.addPlayer())
                );
    }

    @org.junit.jupiter.api.Test
    public void unstackableReward_isGivenOnePerSlot() {
        // #486: 5 totems used to land in one slot as a stack of 5.
        PlayerMock player = server.addPlayer();
        Optional<ItemStack> leftover =
                ItemCalculatorUtil.putItemsIntoInventory(player, new ItemStack(Material.TOTEM_OF_UNDYING, 5));
        Assertions.assertFalse(leftover.isPresent());
        int totems = 0;
        int slots = 0;
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content != null && content.getType() == Material.TOTEM_OF_UNDYING) {
                Assertions.assertEquals(1, content.getAmount(), "a totem slot must hold exactly one totem");
                totems += content.getAmount();
                slots++;
            }
        }
        Assertions.assertEquals(5, totems);
        Assertions.assertEquals(5, slots);
    }

    @org.junit.jupiter.api.Test
    public void unstackableReward_leftoverIsReturnedWhenSlotsRunOut() {
        PlayerMock player = server.addPlayer();
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 2; i < storage.length; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.STONE, 64));
        }
        Optional<ItemStack> leftover =
                ItemCalculatorUtil.putItemsIntoInventory(player, new ItemStack(Material.TOTEM_OF_UNDYING, 5));
        Assertions.assertTrue(leftover.isPresent());
        Assertions.assertEquals(3, leftover.get().getAmount(), "2 free slots hold 2 totems; 3 are left over");
    }

    @org.junit.jupiter.api.Test
    public void split_respectsMaxStackSize() {
        java.util.List<ItemStack> pearls = ItemCalculatorUtil.split(new ItemStack(Material.ENDER_PEARL, 20));
        Assertions.assertEquals(2, pearls.size());
        Assertions.assertEquals(16, pearls.get(0).getAmount());
        Assertions.assertEquals(4, pearls.get(1).getAmount());

        java.util.List<ItemStack> totems = ItemCalculatorUtil.split(new ItemStack(Material.TOTEM_OF_UNDYING, 3));
        Assertions.assertEquals(3, totems.size());
        totems.forEach(t -> Assertions.assertEquals(1, t.getAmount()));
    }
}
