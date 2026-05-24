package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestTargetSelector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class DamageEffectCommands {

    private static final int TEN_MINUTES = 20 * 60 * 10;

    private DamageEffectCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("effect")
                .requires(DamageCommandSecurity.adminPermission())
                .then(Commands.literal("self")
                        .then(Commands.literal("strength")
                                .executes(ctx -> addSelfStrength(ctx.getSource(), 0)))
                        .then(Commands.literal("strength2")
                                .executes(ctx -> addSelfStrength(ctx.getSource(), 1)))
                        .then(Commands.literal("weakness")
                                .executes(ctx -> addSelfWeakness(ctx.getSource(), 0)))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearSelfEffects(ctx.getSource()))))

                .then(Commands.literal("target")
                        .then(Commands.literal("resistance")
                                .executes(ctx -> addTargetResistance(ctx.getSource(), 0)))
                        .then(Commands.literal("resistance2")
                                .executes(ctx -> addTargetResistance(ctx.getSource(), 1)))
                        .then(Commands.literal("burning")
                                .executes(ctx -> igniteNearestTarget(ctx.getSource())))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearTargetEffects(ctx.getSource())))));
    }

    private static int addSelfStrength(
            CommandSourceStack source,
            int amplifier
    ) {
        LivingEntity self = selfLiving(source);

        if (self == null) {
            return CommandFeedback.fail(
                    source,
                    "this command must be run by a living entity."
            );
        }

        self.addEffect(new MobEffectInstance(
                MobEffects.STRENGTH,
                TEN_MINUTES,
                amplifier,
                false,
                true
        ));

        return CommandFeedback.success(
                source,
                "strength applied to self."
        );
    }

    private static int addSelfWeakness(
            CommandSourceStack source,
            int amplifier
    ) {
        LivingEntity self = selfLiving(source);

        if (self == null) {
            return CommandFeedback.fail(
                    source,
                    "this command must be run by a living entity."
            );
        }

        self.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                TEN_MINUTES,
                amplifier,
                false,
                true
        ));

        return CommandFeedback.success(
                source,
                "weakness applied to self."
        );
    }

    private static int clearSelfEffects(CommandSourceStack source) {
        LivingEntity self = selfLiving(source);

        if (self == null) {
            return CommandFeedback.fail(
                    source,
                    "this command must be run by a living entity."
            );
        }

        self.removeAllEffects();

        return CommandFeedback.success(
                source,
                "self effects cleared."
        );
    }

    private static int addTargetResistance(
            CommandSourceStack source,
            int amplifier
    ) {
        LivingEntity target = TestTargetSelector.nearestTestLiving(source);

        if (target == null) {
            return noTarget(source);
        }

        target.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE,
                TEN_MINUTES,
                amplifier,
                false,
                true
        ));

        return CommandFeedback.success(
                source,
                "resistance applied to nearest test target."
        );
    }

    private static int igniteNearestTarget(CommandSourceStack source) {
        LivingEntity target = TestTargetSelector.nearestTestLiving(source);

        if (target == null) {
            return noTarget(source);
        }

        target.igniteForSeconds(30.0F);

        return CommandFeedback.success(
                source,
                "nearest test target ignited."
        );
    }

    private static int clearTargetEffects(CommandSourceStack source) {
        LivingEntity target = TestTargetSelector.nearestTestLiving(source);

        if (target == null) {
            return noTarget(source);
        }

        target.removeAllEffects();

        return CommandFeedback.success(
                source,
                "nearest test target effects cleared."
        );
    }

    private static LivingEntity selfLiving(CommandSourceStack source) {
        return source.getEntity() instanceof LivingEntity living
                ? living
                : null;
    }

    private static int noTarget(CommandSourceStack source) {
        return CommandFeedback.fail(
                source,
                "no nearby [DN-Test] living target found."
        );
    }
}
