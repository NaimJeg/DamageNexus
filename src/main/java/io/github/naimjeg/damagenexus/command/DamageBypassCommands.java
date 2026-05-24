package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestTargetSelector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.LivingEntity;

public final class DamageBypassCommands {

    private DamageBypassCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("bypass")
                .requires(DamageCommandSecurity.adminPermission())
                .then(Commands.literal("health_minus_4")
                        .executes(ctx -> bypassHealthDelta(
                                ctx.getSource(),
                                -4.0f,
                                "direct health -4"
                        )))
                .then(Commands.literal("absorption_minus_4")
                        .executes(ctx -> bypassAbsorptionDelta(
                                ctx.getSource(),
                                -4.0f,
                                "direct absorption -4"
                        ))));
    }

    private static int bypassHealthDelta(
            CommandSourceStack source,
            float delta,
            String label
    ) {
        LivingEntity target = TestTargetSelector.nearestTestLiving(source);

        if (target == null) {
            return CommandFeedback.fail(
                    source,
                    "no nearby [DN-Test] living target found."
            );
        }

        float before = target.getHealth();
        float after = Math.max(
                0.0f,
                Math.min(target.getMaxHealth(), before + delta)
        );

        target.setHealth(after);

        return CommandFeedback.success(
                source,
                label + " applied. health " + before + " -> " + after
        );
    }

    private static int bypassAbsorptionDelta(
            CommandSourceStack source,
            float delta,
            String label
    ) {
        LivingEntity target = TestTargetSelector.nearestTestLiving(source);

        if (target == null) {
            return CommandFeedback.fail(
                    source,
                    "no nearby [DN-Test] living target found."
            );
        }

        float before = target.getAbsorptionAmount();
        float after = Math.max(0.0f, before + delta);

        target.setAbsorptionAmount(after);

        return CommandFeedback.success(
                source,
                label + " applied. absorption " + before + " -> " + after
        );
    }
}
