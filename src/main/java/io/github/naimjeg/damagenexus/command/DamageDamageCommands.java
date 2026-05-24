package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestTargetSelector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                                "lava damage x1"
                        )))
                .then(Commands.literal("on_fire")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().onFire(),
                                1.0f,
                                1,
                                "on-fire damage x1"
                        )))
                .then(Commands.literal("in_fire")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().inFire(),
                                1.0f,
                                1,
                                "in-fire damage x1"
                        )))
                .then(Commands.literal("lava_burst")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().lava(),
                                4.0f,
                                25,
                                "lava damage burst x25"
                        )))
                .then(Commands.literal("on_fire_burst")
                        .executes(ctx -> damageNearest(
                                ctx.getSource(),
                                level -> level.damageSources().onFire(),
                                1.0f,
                                25,
                                "on-fire damage burst x25"
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
                    "no nearby [DN-Test] living target found."
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
                label + " applied to nearest target. attempts="
                        + repeats
                        + ", accepted="
                        + accepted
        );
    }

    @FunctionalInterface
    private interface DamageSourceFactory {
        DamageSource create(ServerLevel level);
    }
}
