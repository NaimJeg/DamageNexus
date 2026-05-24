package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestItemFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class DamageItemCommands {

    private DamageItemCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("item")
                .requires(DamageCommandSecurity.adminPermission())
                .then(Commands.literal("all")
                        .executes(ctx -> giveAllItems(ctx.getSource())))

                .then(Commands.literal("clear")
                        .executes(ctx -> clearTestItems(ctx.getSource())))

                .then(Commands.literal("base")
                        .executes(ctx -> giveBaseKit(ctx.getSource())))

                .then(Commands.literal("entry")
                        .then(Commands.literal("fire")
                                .executes(ctx -> giveEntryFireProbe(ctx.getSource())))
                        .then(Commands.literal("unique_group")
                                .executes(ctx -> giveEntryUniqueGroupProbe(ctx.getSource())))
                        .then(Commands.literal("replace")
                                .executes(ctx -> giveEntryReplaceProbe(ctx.getSource())))
                        .then(Commands.literal("kit")
                                .executes(ctx -> giveEntryProbeKit(ctx.getSource()))))

                .then(Commands.literal("enchant")
                        .then(Commands.literal("sharpness")
                                .executes(ctx -> giveSharpnessSword(ctx.getSource())))
                        .then(Commands.literal("smite")
                                .executes(ctx -> giveSmiteSword(ctx.getSource())))
                        .then(Commands.literal("bane")
                                .executes(ctx -> giveBaneSword(ctx.getSource())))
                        .then(Commands.literal("kit")
                                .executes(ctx -> giveEnchantKit(ctx.getSource()))))

                .then(Commands.literal("channel")
                        .then(Commands.literal("physical_sword")
                                .executes(ctx -> givePhysicalSword(ctx.getSource())))
                        .then(Commands.literal("fire_sword")
                                .executes(ctx -> giveFireSword(ctx.getSource())))
                        .then(Commands.literal("kit")
                                .executes(ctx -> giveChannelKit(ctx.getSource()))))

                .then(Commands.literal("crit")
                        .then(Commands.literal("physical_scaling")
                                .executes(ctx -> giveCriticalPhysicalSword(
                                        ctx.getSource())))
                        .then(Commands.literal("damage_additive")
                                .executes(ctx -> giveCritDamageAdditiveSword(
                                        ctx.getSource())))
                        .then(Commands.literal("kit")
                                .executes(ctx -> giveCritKit(ctx.getSource())))
                        .executes(ctx -> giveCritKit(ctx.getSource())))

                .then(Commands.literal("affix")
                        .then(Commands.literal("blazing_edge")
                                .executes(ctx -> giveBlazingEdgeSword(ctx.getSource())))
                        .then(Commands.literal("unique_group")
                                .executes(ctx -> giveAffixUniqueGroupProbe(ctx.getSource())))
                        .then(Commands.literal("replace")
                                .executes(ctx -> giveAffixReplaceProbe(ctx.getSource())))
                        .then(Commands.literal("highest")
                                .executes(ctx -> giveAffixHighestRarityProbe(ctx.getSource())))
                        .then(Commands.literal("kit")
                                .executes(ctx -> giveAffixKit(ctx.getSource()))))

                .then(Commands.literal("ops")
                        .then(Commands.literal("convert_gain")
                                .executes(ctx -> giveConvertGainOpsItem(ctx.getSource())))
                        .then(Commands.literal("defensive")
                                .executes(ctx -> giveDefensiveOpsItem(ctx.getSource())))
                        .then(Commands.literal("final_override")
                                .executes(ctx -> giveFinalOverrideOpsItem(ctx.getSource())))
                        .then(Commands.literal("multipliers")
                                .executes(ctx -> giveMultiplierOpsItem(ctx.getSource())))
                        .then(Commands.literal("kit")
                                .executes(ctx -> giveOperationKit(ctx.getSource())))));

        DamageProjectileItemCommands.register(root);
    }

    private static int giveAllItems(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> {
                    CommandFeedback.withSuppressedSuccess(() -> {
                        giveBaseKit(source, player);
                        giveEnchantKit(source, player);
                        giveCritKit(source, player);
                        giveChannelKit(source, player);
                        giveProjectileKit(source, player);
                        giveOperationKit(source, player);
                        giveEntryProbeKit(source, player);
                        giveAffixKit(source, player);
                        return 1;
                    });
                    return CommandFeedback.success(
                            source,
                            "command.damagenexus.items_created", 29
                    );
                })
                .orElse(0);
    }

    private static int clearTestItems(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> {
                    Inventory inventory = player.getInventory();
                    int removedItems = 0;

                    // 26.1 Inventory#getContainerSize includes hotbar/main
                    // plus EQUIPMENT_SLOT_MAPPING (armor, offhand, body, saddle).
                    for (int slot = 0;
                            slot < inventory.getContainerSize();
                            slot++) {
                        ItemStack stack = inventory.getItem(slot);
                        if (TestItemFactory.isTestItem(stack)) {
                            removedItems += stack.getCount();
                            inventory.setItem(slot, ItemStack.EMPTY);
                        }
                    }

                    AbstractContainerMenu menu = player.containerMenu;
                    ItemStack carried = menu.getCarried();
                    if (TestItemFactory.isTestItem(carried)) {
                        removedItems += carried.getCount();
                        menu.setCarried(ItemStack.EMPTY);
                    }

                    inventory.setChanged();
                    menu.broadcastChanges();

                    return CommandFeedback.success(
                            source,
                            "command.damagenexus.test_items_cleared",
                            removedItems
                    );
                })
                .orElse(0);
    }

    private static int giveBaseKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveBaseKit(source, player))
                .orElse(0);
    }

    private static int giveBaseKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.plainIronSword());
        give(player, TestItemFactory.plainDiamondSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveSharpnessSword(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveSharpnessSword(source, player))
                .orElse(0);
    }

    private static int giveSharpnessSword(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.sharpnessSword(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveSmiteSword(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveSmiteSword(source, player))
                .orElse(0);
    }

    private static int giveSmiteSword(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.smiteSword(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveBaneSword(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveBaneSword(source, player))
                .orElse(0);
    }

    private static int giveBaneSword(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.baneSword(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveEnchantKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveEnchantKit(source, player))
                .orElse(0);
    }

    private static int giveEnchantKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.sharpnessSword(source.getLevel()));
        give(player, TestItemFactory.smiteSword(source.getLevel()));
        give(player, TestItemFactory.baneSword(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 3
        );
    }

    private static int givePhysicalSword(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> givePhysicalSword(source, player))
                .orElse(0);
    }

    private static int givePhysicalSword(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.physicalScalingSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveFireSword(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveFireSword(source, player))
                .orElse(0);
    }

    private static int giveFireSword(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.flatFireSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveChannelKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveChannelKit(source, player))
                .orElse(0);
    }

    private static int giveChannelKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.physicalScalingSword());
        give(player, TestItemFactory.flatFireSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveCritKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveCritKit(source, player))
                .orElse(0);
    }

    private static int giveCritKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.criticalPhysicalScalingSword());
        give(player, TestItemFactory.critDamageAdditiveSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveCriticalPhysicalSword(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> {
                    give(player, TestItemFactory.criticalPhysicalScalingSword());
                    return CommandFeedback.success(
                            source,
                            "command.damagenexus.items_created",
                            1
                    );
                })
                .orElse(0);
    }

    private static int giveCritDamageAdditiveSword(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> {
                    give(player, TestItemFactory.critDamageAdditiveSword());
                    return CommandFeedback.success(
                            source,
                            "command.damagenexus.items_created",
                            1
                    );
                })
                .orElse(0);
    }

    private static int giveBlazingEdgeSword(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveBlazingEdgeSword(source, player))
                .orElse(0);
    }

    private static int giveBlazingEdgeSword(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.blazingEdgeSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveAffixKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveAffixKit(source, player))
                .orElse(0);
    }

    private static int giveAffixKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.blazingEdgeSword());
        give(player, TestItemFactory.affixUniqueGroupProbe());
        give(player, TestItemFactory.affixReplaceProbe());
        give(player, TestItemFactory.affixHighestRarityProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 4
        );
    }

    private static int giveConvertGainOpsItem(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveConvertGainOpsItem(source, player))
                .orElse(0);
    }

    private static int giveConvertGainOpsItem(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.convertGainOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveDefensiveOpsItem(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveDefensiveOpsItem(source, player))
                .orElse(0);
    }

    private static int giveDefensiveOpsItem(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.defensiveOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveFinalOverrideOpsItem(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveFinalOverrideOpsItem(source, player))
                .orElse(0);
    }

    private static int giveFinalOverrideOpsItem(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.finalOverrideOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveMultiplierOpsItem(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveMultiplierOpsItem(source, player))
                .orElse(0);
    }

    private static int giveMultiplierOpsItem(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.multiplierOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveOperationKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveOperationKit(source, player))
                .orElse(0);
    }

    private static int giveOperationKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.convertGainOpsItem());
        give(player, TestItemFactory.defensiveOpsItem());
        give(player, TestItemFactory.finalOverrideOpsItem());
        give(player, TestItemFactory.multiplierOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 4
        );
    }

    private static int giveProjectileKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveProjectileKit(source, player))
                .orElse(0);
    }

    private static int giveProjectileKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.powerBow(source.getLevel()));
        give(player, TestItemFactory.ruleBow());

        give(player, TestItemFactory.plainCrossbow());
        give(player, TestItemFactory.piercingCrossbow(source.getLevel()));
        give(player, TestItemFactory.ruleCrossbow());

        give(player, TestItemFactory.plainTrident());
        give(player, TestItemFactory.impalingTrident(source.getLevel()));
        give(player, TestItemFactory.ruleTrident());

        give(player, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 9
        );
    }

    private static int giveEntryProbeKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveEntryProbeKit(source, player))
                .orElse(0);
    }

    private static int giveEntryProbeKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.entryFireSword());
        give(player, TestItemFactory.entryUniqueGroupProbe());
        give(player, TestItemFactory.entryReplaceProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 3
        );
    }

    private static int giveEntryFireProbe(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> {
                    give(player, TestItemFactory.entryFireSword());
                    return CommandFeedback.success(
                            source,
                            "command.damagenexus.items_created",
                            1
                    );
                })
                .orElse(0);
    }

    private static int giveEntryUniqueGroupProbe(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveEntryUniqueGroupProbe(source, player))
                .orElse(0);
    }

    private static int giveEntryUniqueGroupProbe(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.entryUniqueGroupProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveEntryReplaceProbe(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveEntryReplaceProbe(source, player))
                .orElse(0);
    }

    private static int giveEntryReplaceProbe(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.entryReplaceProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveAffixUniqueGroupProbe(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveAffixUniqueGroupProbe(source, player))
                .orElse(0);
    }

    private static int giveAffixUniqueGroupProbe(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.affixUniqueGroupProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveAffixReplaceProbe(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveAffixReplaceProbe(source, player))
                .orElse(0);
    }

    private static int giveAffixReplaceProbe(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.affixReplaceProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveAffixHighestRarityProbe(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveAffixHighestRarityProbe(source, player))
                .orElse(0);
    }

    private static int giveAffixHighestRarityProbe(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.affixHighestRarityProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
    }
}
