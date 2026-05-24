package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestItemFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
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
        return CommandFeedback.requirePlayer(source)
                .map(player -> givePowerBow(source, player))
                .orElse(0);
    }

    private static int givePowerBow(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.powerBow(source.getLevel()));
        give(player, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveRuleBow(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveRuleBow(source, player))
                .orElse(0);
    }

    private static int giveRuleBow(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.ruleBow());
        give(player, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveCrossbowKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveCrossbowKit(source, player))
                .orElse(0);
    }

    private static int giveCrossbowKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.plainCrossbow());
        give(player, TestItemFactory.piercingCrossbow(source.getLevel()));
        give(player, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 3
        );
    }

    private static int giveRuleCrossbow(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveRuleCrossbow(source, player))
                .orElse(0);
    }

    private static int giveRuleCrossbow(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.ruleCrossbow());
        give(player, TestItemFactory.arrows64());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveTridentKit(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveTridentKit(source, player))
                .orElse(0);
    }

    private static int giveTridentKit(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.plainTrident());
        give(player, TestItemFactory.impalingTrident(source.getLevel()));

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 2
        );
    }

    private static int giveRuleTrident(CommandSourceStack source) {
        return CommandFeedback.requirePlayer(source)
                .map(player -> giveRuleTrident(source, player))
                .orElse(0);
    }

    private static int giveRuleTrident(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        give(player, TestItemFactory.ruleTrident());

        return CommandFeedback.success(
                source,
                "command.damagenexus.items_created", 1
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

    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
    }
}
