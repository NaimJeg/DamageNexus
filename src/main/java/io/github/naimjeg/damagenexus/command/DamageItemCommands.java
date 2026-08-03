package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestItemFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

                .then(Commands.literal("base")
                        .executes(ctx -> giveBaseKit(ctx.getSource())))

                .then(Commands.literal("entry")
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
                        .executes(ctx -> giveCritKit(ctx.getSource())))

                .then(Commands.literal("affix")
                        .then(Commands.literal("blazing_edge")
                                .executes(ctx -> giveBlazingEdgeSword(ctx.getSource())))
                        .then(Commands.literal("unique_group")
                                .executes(ctx -> giveAffixUniqueGroupProbe(ctx.getSource())))
                        .then(Commands.literal("replace")
                                .executes(ctx -> giveAffixReplaceProbe(ctx.getSource())))
                        .then(Commands.literal("highest")
                                .executes(ctx -> giveAffixHighestLevelProbe(ctx.getSource())))
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
        CommandFeedback.withSuppressedSuccess(() -> {
            giveBaseKit(source);
            giveEnchantKit(source);
            giveCritKit(source);
            giveChannelKit(source);
            giveProjectileKit(source);
            giveOperationKit(source);
            giveEntryProbeKit(source);
            giveAffixKit(source);
            return 1;
        });

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 27
        );
    }

    private static int giveBaseKit(CommandSourceStack source) {
        give(source, TestItemFactory.plainIronSword());
        give(source, TestItemFactory.plainDiamondSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveSharpnessSword(CommandSourceStack source) {
        give(source, TestItemFactory.sharpnessSword(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveSmiteSword(CommandSourceStack source) {
        give(source, TestItemFactory.smiteSword(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveBaneSword(CommandSourceStack source) {
        give(source, TestItemFactory.baneSword(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveEnchantKit(CommandSourceStack source) {
        give(source, TestItemFactory.sharpnessSword(source.getLevel()));
        give(source, TestItemFactory.smiteSword(source.getLevel()));
        give(source, TestItemFactory.baneSword(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 3
        );
    }

    private static int givePhysicalSword(CommandSourceStack source) {
        give(source, TestItemFactory.physicalScalingSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveFireSword(CommandSourceStack source) {
        give(source, TestItemFactory.flatFireSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveChannelKit(CommandSourceStack source) {
        give(source, TestItemFactory.physicalScalingSword());
        give(source, TestItemFactory.flatFireSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveCritKit(CommandSourceStack source) {
        give(source, TestItemFactory.critDamageSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveBlazingEdgeSword(CommandSourceStack source) {
        give(source, TestItemFactory.blazingEdgeSword());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveAffixKit(CommandSourceStack source) {
        give(source, TestItemFactory.blazingEdgeSword());
        give(source, TestItemFactory.affixUniqueGroupProbe());
        give(source, TestItemFactory.affixReplaceProbe());
        give(source, TestItemFactory.affixHighestLevelProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 4
        );
    }

    private static int giveConvertGainOpsItem(CommandSourceStack source) {
        give(source, TestItemFactory.convertGainOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveDefensiveOpsItem(CommandSourceStack source) {
        give(source, TestItemFactory.defensiveOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveFinalOverrideOpsItem(CommandSourceStack source) {
        give(source, TestItemFactory.finalOverrideOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveMultiplierOpsItem(CommandSourceStack source) {
        give(source, TestItemFactory.multiplierOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveOperationKit(CommandSourceStack source) {
        give(source, TestItemFactory.convertGainOpsItem());
        give(source, TestItemFactory.defensiveOpsItem());
        give(source, TestItemFactory.finalOverrideOpsItem());
        give(source, TestItemFactory.multiplierOpsItem());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 4
        );
    }

    private static int giveProjectileKit(CommandSourceStack source) {
        give(source, TestItemFactory.powerBow(source.getLevel()));
        give(source, TestItemFactory.ruleBow());

        give(source, TestItemFactory.plainCrossbow());
        give(source, TestItemFactory.piercingCrossbow(source.getLevel()));
        give(source, TestItemFactory.ruleCrossbow());

        give(source, TestItemFactory.plainTrident());
        give(source, TestItemFactory.impalingTrident(source.getLevel()));
        give(source, TestItemFactory.ruleTrident());

        give(source, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 9
        );
    }

    private static int giveEntryProbeKit(CommandSourceStack source) {
        give(source, TestItemFactory.entryUniqueGroupProbe());
        give(source, TestItemFactory.entryReplaceProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveEntryUniqueGroupProbe(CommandSourceStack source) {
        give(source, TestItemFactory.entryUniqueGroupProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveEntryReplaceProbe(CommandSourceStack source) {
        give(source, TestItemFactory.entryReplaceProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveAffixUniqueGroupProbe(CommandSourceStack source) {
        give(source, TestItemFactory.affixUniqueGroupProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveAffixReplaceProbe(CommandSourceStack source) {
        give(source, TestItemFactory.affixReplaceProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static int giveAffixHighestLevelProbe(CommandSourceStack source) {
        give(source, TestItemFactory.affixHighestLevelProbe());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
        );
    }

    private static void give(
            CommandSourceStack source,
            ItemStack stack
    ) {
        if (source.getPlayer() == null) {
            return;
        }

        source.getPlayer().getInventory().add(stack);
    }
}
