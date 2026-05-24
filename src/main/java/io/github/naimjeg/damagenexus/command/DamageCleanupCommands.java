package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory;
import io.github.naimjeg.damagenexus.command.test.TestMobTags;
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
            forceRemoveTestEntity(entity);
        });

        Entity executor = source.getEntity();

        if (executor instanceof LivingEntity living) {
            TestMobFactory.sanitizePlayer(living);
        }

        return CommandFeedback.success(
                source,
                "command.damagenexus.cleanup_complete"
        );
    }

    static boolean forceRemoveTestEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        Component customName = entity.getCustomName();
        if (!TestMobTags.isTestEntity(entity)
                && !TestTargetSelector.isTestEntityName(customName)) {
            return false;
        }

        entity.removeTag(TestMobTags.IMMORTAL);
        entity.removeTag(TestMobTags.PENDING_IMMORTAL_RESTORE);
        entity.discard();
        return true;
    }
}
