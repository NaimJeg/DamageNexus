package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory.SpawnResult;
import io.github.naimjeg.damagenexus.command.test.TestMobPreset;
import io.github.naimjeg.damagenexus.command.test.TestMobSpawnOptions;
import io.github.naimjeg.damagenexus.command.test.TestMobTags;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Locale;

public final class DamageMobCommands {

    private DamageMobCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        LiteralArgumentBuilder<CommandSourceStack> mob =
                Commands.literal("mob")
                        .requires(DamageCommandSecurity.adminPermission());

        for (TestMobPreset preset : TestMobPreset.values()) {
            mob.then(presetBranch(preset));
        }

        mob.then(Commands.literal("mortalize")
                .then(Commands.argument(
                                "targets",
                                EntityArgument.entities()
                        )
                        .executes(ctx -> mortalize(
                                ctx.getSource(),
                                EntityArgument.getEntities(
                                        ctx,
                                        "targets"
                                )
                        ))));
        root.then(mob);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> presetBranch(
            TestMobPreset preset
    ) {
        return Commands.literal(preset.commandName())
                .executes(ctx -> guarded(
                        ctx.getSource(),
                        () -> spawnTestMob(
                                ctx.getSource(),
                                preset,
                                ctx.getSource().getPosition().add(2, 0, 0),
                                TestMobSpawnOptions.DEFAULT
                        )
                ))
                .then(Commands.literal("at")
                        .then(Commands.argument(
                                        "pos",
                                        Vec3Argument.vec3()
                                )
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnTestMob(
                                                ctx.getSource(),
                                                preset,
                                                Vec3Argument.getVec3(
                                                        ctx,
                                                        "pos"
                                                ),
                                                TestMobSpawnOptions.DEFAULT
                                        )
                                ))
                                .then(Commands.literal("immortal")
                                        .executes(ctx -> guarded(
                                                ctx.getSource(),
                                                () -> spawnTestMob(
                                                        ctx.getSource(),
                                                        preset,
                                                        Vec3Argument.getVec3(
                                                                ctx,
                                                                "pos"
                                                        ),
                                                        TestMobSpawnOptions
                                                                .IMMORTAL_EXHIBIT
                                                )
                                        )))));
    }

    private static int guarded(
            CommandSourceStack source,
            java.util.function.IntSupplier command
    ) {
        return DamageCommandSecurity.runWithCooldown(
                source,
                DamageCommandSecurity.ExpensiveAction.SPAWN_ENTITIES,
                command
        );
    }

    static int spawnTestMob(
            CommandSourceStack source,
            TestMobPreset preset,
            Vec3 position,
            TestMobSpawnOptions options
    ) {
        SpawnResult result = TestMobFactory.spawnPreset(
                source.getLevel(),
                preset,
                position,
                options
        );
        String formattedPosition = formatPosition(position);

        if (!result.succeeded()) {
            return CommandFeedback.fail(
                    source,
                    "command.damagenexus.mob_spawn_failed",
                    preset.commandName(),
                    formattedPosition,
                    Component.translatable(
                            result.failure().translationKey()
                    )
            );
        }

        return CommandFeedback.success(
                source,
                "command.damagenexus.mob_spawned",
                preset.commandName(),
                formattedPosition,
                result.entity().getUUID().toString(),
                options.immortal()
        );
    }

    private static int mortalize(
            CommandSourceStack source,
            Collection<? extends Entity> targets
    ) {
        int changed = 0;
        for (Entity target : targets) {
            if (target.removeTag(TestMobTags.IMMORTAL)) {
                target.removeTag(TestMobTags.PENDING_IMMORTAL_RESTORE);
                changed++;
            }
        }

        if (changed == 0) {
            return CommandFeedback.fail(
                    source,
                    "command.damagenexus.mortalize_none"
            );
        }
        return CommandFeedback.successCount(
                source,
                changed,
                "command.damagenexus.mortalized",
                changed
        );
    }

    static String formatPosition(Vec3 position) {
        return String.format(
                Locale.ROOT,
                "%.3f %.3f %.3f",
                position.x,
                position.y,
                position.z
        );
    }
}
