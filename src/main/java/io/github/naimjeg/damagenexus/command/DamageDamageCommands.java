package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestTargetSelector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class DamageDamageCommands {

    private DamageDamageCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("damage")
                .requires(DamageCommandSecurity.adminPermission())
                .then(Commands.literal("lava")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().lava(),
                                4.0f,
                                1,
                                "command.damagenexus.damage.lava"
                        )))
                .then(Commands.literal("on_fire")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().onFire(),
                                1.0f,
                                1,
                                "command.damagenexus.damage.on_fire"
                        )))
                .then(Commands.literal("in_fire")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().inFire(),
                                1.0f,
                                1,
                                "command.damagenexus.damage.in_fire"
                        )))
                .then(Commands.literal("lava_burst")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().lava(),
                                4.0f,
                                25,
                                "command.damagenexus.damage.lava_burst"
                        )))
                .then(Commands.literal("on_fire_burst")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().onFire(),
                                1.0f,
                                25,
                                "command.damagenexus.damage.on_fire_burst"
                        ))));
    }

    private static int damageNearest(
            CommandSourceStack source,
            DamageSourceFactory damageSourceFactory,
            float amount,
            int repeats,
            String label
    ) {
        LivingEntity target = TestTargetSelector.nearestTestLiving(source);

        if (target == null) {
            return CommandFeedback.fail(
                    source,
                    "command.damagenexus.target_not_found"
            );
        }

        ServerLevel level = source.getLevel();
        DamageSource damageSource = damageSourceFactory.create(level);

        int accepted = 0;

        for (int i = 0; i < repeats; i++) {
            if (target.hurtServer(level, damageSource, amount)) {
                accepted++;
            }
        }

        return CommandFeedback.success(
                source,
                "command.damagenexus.damage.applied",
                Component.translatable(label), repeats, accepted
        );
    }

    @FunctionalInterface
    private interface DamageSourceFactory {
        DamageSource create(ServerLevel level);
    }
}
