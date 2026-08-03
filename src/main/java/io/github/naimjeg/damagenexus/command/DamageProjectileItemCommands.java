package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestItemFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.ItemStack;

public final class DamageProjectileItemCommands {

    private DamageProjectileItemCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("projectile")
                .requires(DamageCommandSecurity.adminPermission())
                .then(Commands.literal("power_bow")
                        .executes(ctx -> givePowerBow(ctx.getSource())))
                .then(Commands.literal("rule_bow")
                        .executes(ctx -> giveRuleBow(ctx.getSource())))
                .then(Commands.literal("crossbow")
                        .executes(ctx -> giveCrossbowKit(ctx.getSource())))
                .then(Commands.literal("rule_crossbow")
                        .executes(ctx -> giveRuleCrossbow(ctx.getSource())))
                .then(Commands.literal("trident")
                        .executes(ctx -> giveTridentKit(ctx.getSource())))
                .then(Commands.literal("rule_trident")
                        .executes(ctx -> giveRuleTrident(ctx.getSource())))
                .then(Commands.literal("kit")
                        .executes(ctx -> giveProjectileKit(ctx.getSource()))));
    }

    private static int givePowerBow(CommandSourceStack source) {
        give(source, TestItemFactory.powerBow(source.getLevel()));
        give(source, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveRuleBow(CommandSourceStack source) {
        give(source, TestItemFactory.ruleBow());
        give(source, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveCrossbowKit(CommandSourceStack source) {
        give(source, TestItemFactory.plainCrossbow());
        give(source, TestItemFactory.piercingCrossbow(source.getLevel()));
        give(source, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 3
        );
    }

    private static int giveRuleCrossbow(CommandSourceStack source) {
        give(source, TestItemFactory.ruleCrossbow());
        give(source, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveTridentKit(CommandSourceStack source) {
        give(source, TestItemFactory.plainTrident());
        give(source, TestItemFactory.impalingTrident(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveRuleTrident(CommandSourceStack source) {
        give(source, TestItemFactory.ruleTrident());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
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
