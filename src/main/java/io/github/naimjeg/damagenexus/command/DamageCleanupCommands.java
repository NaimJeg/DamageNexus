package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory;
import io.github.naimjeg.damagenexus.command.test.TestTargetSelector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class DamageCleanupCommands {

    private DamageCleanupCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("cleanup")
                .requires(DamageCommandSecurity.adminPermission())
                .executes(ctx -> DamageCommandSecurity.runWithCooldown(
                        ctx.getSource(),
                        DamageCommandSecurity.ExpensiveAction.CLEANUP,
                        () -> cleanup(ctx.getSource())
                )));
    }

    private static int cleanup(CommandSourceStack source) {
        source.getLevel().getEntities().getAll().forEach(entity -> {
            if (entity == null) {
                return;
            }

            Component customName = entity.getCustomName();

            if (TestTargetSelector.isTestEntityName(customName)) {
                entity.discard();
            }
        });

        Entity executor = source.getEntity();

        if (executor instanceof LivingEntity living) {
            TestMobFactory.sanitizePlayer(living);
        }

        return CommandFeedback.success(
                source,
                "test entities cleaned up."
        );
    }
}
