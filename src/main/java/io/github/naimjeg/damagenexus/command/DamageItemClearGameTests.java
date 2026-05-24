package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.command.test.TestItemFactory;
import io.github.naimjeg.damagenexus.core.gametest.GameTestCodecVerifier;
import io.github.naimjeg.damagenexus.core.gametest.GameTestServerPlayerFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageItemClearGameTests {

    private static final ResourceKey<Consumer<GameTestHelper>>
            ITEM_CLEAR_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("item_clear_semantics")
    );

    private DamageItemClearGameTests() {
    }

    @SubscribeEvent
    public static void registerTestFunction(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                ITEM_CLEAR_FUNCTION.identifier(),
                () -> DamageItemClearGameTests::itemClearSemantics
        );
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("item_clear_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(
                        environment,
                        Identifier.withDefaultNamespace("empty"),
                        20,
                        0,
                        true,
                        Rotation.NONE
                );

        event.registerTest(
                id("item_clear_semantics"),
                new FunctionGameTestInstance(
                        ITEM_CLEAR_FUNCTION,
                        data
                )
        );
    }

    private static void itemClearSemantics(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        verifyFactoryMarkers(helper);

        ServerPlayer player = GameTestServerPlayerFactory.create(helper);
        try {
            verifyGiveCommands(helper, player);
            verifyClearPreservesOrdinaryItems(player, helper);
            DamageNexus.LOGGER.info(
                    "[DamageNexus] Item clear GameTest passed"
            );
            helper.succeed();
        } finally {
            helper.getLevel().getServer().getPlayerList()
                    .deop(player.nameAndId());
        }
    }

    private static void verifyGiveCommands(
            GameTestHelper helper,
            ServerPlayer player
    ) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                commandDispatcher();

        CommandSourceStack blockSource = Commands.createCompilationContext(
                PermissionSet.ALL_PERMISSIONS
        );
        int blockResult = execute(
                dispatcher,
                "damagenexus item channel kit",
                blockSource
        );
        if (blockResult != 0) {
            throw new AssertionError(
                    "Command block item give falsely succeeded: "
                            + blockResult
            );
        }

        CommandSourceStack playerSource = player.createCommandSourceStack()
                .withPermission(PermissionSet.ALL_PERMISSIONS);
        int playerResult = execute(
                dispatcher,
                "damagenexus item channel kit",
                playerSource
        );
        if (playerResult != 1 || !containsTestItem(player.getInventory())) {
            throw new AssertionError(
                    "execute-as-player item give did not deliver the kit"
            );
        }

        player.getInventory().clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);

        int blockProjectileResult = execute(
                dispatcher,
                "damagenexus projectile kit",
                blockSource
        );
        if (blockProjectileResult != 0) {
            throw new AssertionError(
                    "Command block projectile give falsely succeeded: "
                            + blockProjectileResult
            );
        }

        int playerProjectileResult = execute(
                dispatcher,
                "damagenexus projectile kit",
                playerSource
        );
        if (playerProjectileResult != 1
                || !containsTestItem(player.getInventory())
                || !containsProjectileItem(player.getInventory())) {
            throw new AssertionError(
                    "execute-as-player projectile give did not deliver the kit"
            );
        }

        player.getInventory().clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);
    }

    private static void verifyFactoryMarkers(GameTestHelper helper) {
        List<ItemStack> stacks = List.of(
                TestItemFactory.plainIronSword(),
                TestItemFactory.plainDiamondSword(),
                TestItemFactory.physicalScalingSword(),
                TestItemFactory.flatFireSword(),
                TestItemFactory.critDamageSword(),
                TestItemFactory.blazingEdgeSword(),
                TestItemFactory.convertGainOpsItem(),
                TestItemFactory.defensiveOpsItem(),
                TestItemFactory.finalOverrideOpsItem(),
                TestItemFactory.multiplierOpsItem(),
                TestItemFactory.arrows64(),
                TestItemFactory.plainCrossbow(),
                TestItemFactory.plainTrident(),
                TestItemFactory.ruleBow(),
                TestItemFactory.ruleCrossbow(),
                TestItemFactory.ruleTrident(),
                TestItemFactory.entryFireSword(),
                TestItemFactory.entryUniqueGroupProbe(),
                TestItemFactory.entryReplaceProbe(),
                TestItemFactory.affixUniqueGroupProbe(),
                TestItemFactory.affixReplaceProbe(),
                TestItemFactory.affixHighestLevelProbe(),
                TestItemFactory.powerBow(helper.getLevel()),
                TestItemFactory.piercingCrossbow(helper.getLevel()),
                TestItemFactory.impalingTrident(helper.getLevel()),
                TestItemFactory.sharpnessSword(helper.getLevel()),
                TestItemFactory.smiteSword(helper.getLevel()),
                TestItemFactory.baneSword(helper.getLevel())
        );

        for (ItemStack stack : stacks) {
            if (!TestItemFactory.isTestItem(stack)) {
                throw new AssertionError(
                        "Factory item missing test marker: "
                                + stack
                );
            }
        }

        ItemStack namedOrdinary = new ItemStack(Items.IRON_SWORD);
        namedOrdinary.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("[DN-Test] Fake")
        );
        if (TestItemFactory.isTestItem(namedOrdinary)
                || TestItemFactory.isTestItem(new ItemStack(Items.IRON_SWORD))
                || TestItemFactory.isTestItem(new ItemStack(Items.ARROW))) {
            throw new AssertionError(
                    "Ordinary items were identified as DamageNexus test items"
            );
        }
    }

    private static void verifyClearPreservesOrdinaryItems(
            ServerPlayer player,
            GameTestHelper helper
    ) {
        Inventory inventory = player.getInventory();
        ItemStack ordinaryDiamond = new ItemStack(Items.DIAMOND);
        ItemStack ordinaryShield = new ItemStack(Items.SHIELD);
        ItemStack fakeNamedSword = new ItemStack(Items.IRON_SWORD);
        fakeNamedSword.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("[DN-Test] Fake")
        );

        inventory.setItem(0, ordinaryDiamond);
        inventory.setItem(1, TestItemFactory.plainIronSword());
        inventory.setItem(9, TestItemFactory.arrows64());
        inventory.setItem(10, fakeNamedSword);
        player.setItemSlot(EquipmentSlot.HEAD, TestItemFactory.plainDiamondSword());
        player.setItemSlot(EquipmentSlot.OFFHAND, TestItemFactory.defensiveOpsItem());
        player.setItemSlot(EquipmentSlot.CHEST, ordinaryShield);
        player.setItemSlot(EquipmentSlot.LEGS, ordinaryShield);
        player.containerMenu.setCarried(TestItemFactory.arrows64());

        CommandDispatcher<CommandSourceStack> dispatcher =
                commandDispatcher();

        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(PermissionSet.ALL_PERMISSIONS);
        int result = execute(
                dispatcher,
                "damagenexus item clear",
                source
        );

        if (result != 1) {
            throw new AssertionError(
                    "Item clear command returned failure: " + result
            );
        }

        assertSameStack(inventory.getItem(0), ordinaryDiamond);
        assertEmpty(inventory.getItem(1), 1);
        assertEmpty(inventory.getItem(9), 9);
        assertSameStack(inventory.getItem(10), fakeNamedSword);
        assertEmpty(player.getItemBySlot(EquipmentSlot.HEAD), "head");
        assertEmpty(player.getItemBySlot(EquipmentSlot.OFFHAND), "offhand");
        assertSameStack(
                player.getItemBySlot(EquipmentSlot.CHEST),
                ordinaryShield
        );
        assertSameStack(
                player.getItemBySlot(EquipmentSlot.LEGS),
                ordinaryShield
        );
        assertEmpty(player.containerMenu.getCarried(), "carried");
    }

    private static CommandDispatcher<CommandSourceStack> commandDispatcher() {
        CommandDispatcher<CommandSourceStack> dispatcher =
                new CommandDispatcher<>();
        var root = Commands.literal("damagenexus");
        DamageItemCommands.register(root);
        dispatcher.register(root);
        return dispatcher;
    }

    private static int execute(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String command,
            CommandSourceStack source
    ) {
        try {
            return dispatcher.execute(command, source);
        } catch (Exception exception) {
            throw new AssertionError(
                    "Command execution failed: " + command,
                    exception
            );
        }
    }

    private static boolean containsTestItem(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (TestItemFactory.isTestItem(inventory.getItem(slot))) {
                return true;
            }
        }
        return TestItemFactory.isTestItem(inventory.player.containerMenu.getCarried());
    }

    private static boolean containsProjectileItem(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var item = inventory.getItem(slot).getItem();
            if (item == Items.BOW
                    || item == Items.CROSSBOW
                    || item == Items.TRIDENT
                    || item == Items.ARROW) {
                return true;
            }
        }
        return false;
    }

    private static void assertSameStack(ItemStack actual, ItemStack expected) {
        if (actual.getItem() != expected.getItem()) {
            throw new AssertionError(
                    "Ordinary item changed: expected "
                            + expected
                            + " but got "
                            + actual
            );
        }
    }

    private static void assertEmpty(ItemStack stack, Object slot) {
        if (!stack.isEmpty()) {
            throw new AssertionError(
                    "Test item remained in slot "
                            + slot
                            + ": "
                            + stack
            );
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DamageNexus.MODID, path);
    }
}
