package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory.SpawnResult;
import io.github.naimjeg.damagenexus.command.test.TestMobPreset;
import io.github.naimjeg.damagenexus.command.test.ResistanceMutation;
import io.github.naimjeg.damagenexus.command.test.TestMobSpawnConfig;
import io.github.naimjeg.damagenexus.command.test.TestMobSpawnOptions;
import io.github.naimjeg.damagenexus.command.test.TestMobTags;
import io.github.naimjeg.damagenexus.command.test.TestResistance;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
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
        PositionSource defaultPosition =
                context -> context.getSource().getPosition().add(2, 0, 0);

        ArgumentBuilder<CommandSourceStack, ?> positionNode =
                Commands.argument("pos", Vec3Argument.vec3())
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnTestMob(
                                        ctx.getSource(),
                                        preset,
                                        Vec3Argument.getVec3(ctx, "pos"),
                                        TestMobSpawnConfig.defaults()
                                )
                        ))
                        .then(mutationSubtree(
                                preset,
                                context -> Vec3Argument.getVec3(
                                        context,
                                        "pos"
                                ),
                                TestMobSpawnOptions.DEFAULT
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
                                                new TestMobSpawnConfig(
                                                        TestMobSpawnOptions
                                                                .IMMORTAL_EXHIBIT,
                                                        List.of()
                                                )
                                        )
                                ))
                                .then(mutationSubtree(
                                        preset,
                                        context -> Vec3Argument.getVec3(
                                                context,
                                                "pos"
                                        ),
                                        TestMobSpawnOptions.IMMORTAL_EXHIBIT
                                )));

        return Commands.literal(preset.commandName())
                .executes(ctx -> guarded(
                        ctx.getSource(),
                        () -> spawnTestMob(
                                ctx.getSource(),
                                preset,
                                defaultPosition.resolve(ctx),
                                TestMobSpawnConfig.defaults()
                        )
                ))
                .then(Commands.literal("at")
                        .then(positionNode))
                .then(mutationSubtree(
                        preset,
                        defaultPosition,
                        TestMobSpawnOptions.DEFAULT
                ));
    }

    /**
     * Builds one fresh {@code mutation resistance <type> <value>} subtree for
     * the given preset. A new builder is deliberately allocated per parent so
     * Brigadier nodes are never reused across two parents.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> mutationSubtree(
            TestMobPreset preset,
            PositionSource positionSource,
            TestMobSpawnOptions options
    ) {
        return Commands.literal("mutation")
                .then(resistanceSubtree(preset, positionSource, options));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> resistanceSubtree(
            TestMobPreset preset,
            PositionSource positionSource,
            TestMobSpawnOptions options
    ) {
        LiteralArgumentBuilder<CommandSourceStack> resistance =
                Commands.literal("resistance");

        for (TestResistance testResistance : TestResistance.values()) {
            resistance.then(resistanceValueBranch(
                    preset,
                    positionSource,
                    options,
                    testResistance
            ));
        }

        return resistance;
    }

    private static LiteralArgumentBuilder<CommandSourceStack>
    resistanceValueBranch(
            TestMobPreset preset,
            PositionSource positionSource,
            TestMobSpawnOptions options,
            TestResistance resistance
    ) {
        return Commands.literal(resistance.commandName())
                .then(Commands.argument(
                                "value",
                                DoubleArgumentType.doubleArg(
                                        ModAttributes.RESISTANCE_RATING_MIN,
                                        ModAttributes.RESISTANCE_RATING_MAX
                                )
                        )
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnTestMob(
                                        ctx.getSource(),
                                        preset,
                                        positionSource.resolve(ctx),
                                        new TestMobSpawnConfig(
                                                options,
                                                List.of(new ResistanceMutation(
                                                        resistance,
                                                        DoubleArgumentType
                                                                .getDouble(
                                                                        ctx,
                                                                        "value"
                                                                )
                                                ))
                                        )
                                )
                        )));
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
        return spawnTestMob(
                source,
                preset,
                position,
                new TestMobSpawnConfig(options, List.of())
        );
    }

    static int spawnTestMob(
            CommandSourceStack source,
            TestMobPreset preset,
            Vec3 position,
            TestMobSpawnConfig config
    ) {
        SpawnResult result = TestMobFactory.spawnPreset(
                source.getLevel(),
                preset,
                position,
                config
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
                config.options().immortal()
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

    @FunctionalInterface
    private interface PositionSource {
        Vec3 resolve(CommandContext<CommandSourceStack> context);
    }
}
