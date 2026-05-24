package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory.ArmorSet;
import io.github.naimjeg.damagenexus.command.test.TestMobSpawnOptions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class DamageTestCommands {

    private static final int TEN_MINUTES = 20 * 60 * 10;

    private DamageTestCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("test")
                .requires(DamageCommandSecurity.adminPermission())
                .then(Commands.literal("all")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> runAll(ctx.getSource())
                        )))

                .then(Commands.literal("targets")
                        .then(Commands.literal("all")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnAllTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("defense")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnDefenseTargets(ctx.getSource())
                                ))
                                .then(Commands.literal("at")
                                        .then(Commands.argument(
                                                        "pos",
                                                        Vec3Argument.vec3()
                                                )
                                                .executes(ctx -> guarded(
                                                        ctx.getSource(),
                                                        () -> spawnDefenseTargets(
                                                                ctx.getSource(),
                                                                Vec3Argument.getVec3(
                                                                        ctx,
                                                                        "pos"
                                                                ),
                                                                TestMobSpawnOptions.DEFAULT,
                                                                true
                                                        )
                                                ))
                                                .then(Commands.literal("immortal")
                                                        .executes(ctx -> guarded(
                                                                ctx.getSource(),
                                                                () -> spawnDefenseTargets(
                                                                        ctx.getSource(),
                                                                        Vec3Argument.getVec3(
                                                                                ctx,
                                                                                "pos"
                                                                        ),
                                                                        TestMobSpawnOptions.IMMORTAL_EXHIBIT,
                                                                        true
                                                                )
                                                        ))))))
                        .then(Commands.literal("enchant")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnEnchantTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("effects")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnEffectTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("post")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnInvulTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("environmental")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnEnvironmentalTargets(ctx.getSource())
                                ))))

                .then(Commands.literal("bridge")
                        .then(Commands.literal("all")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnBridgeTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("projectile")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnProjectileTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("mace")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnMaceTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("spear")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnSpearTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("trident")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnTridentTargets(ctx.getSource())
                                )))
                        .then(Commands.literal("mob_difficulty")
                                .executes(ctx -> guarded(
                                        ctx.getSource(),
                                        () -> spawnMobDifficultyTargets(ctx.getSource())
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

    private static int runAll(CommandSourceStack source) {
        CommandFeedback.withSuppressedSuccess(() -> {
            spawnAllTargets(source);
            spawnBridgeTargets(source);
            return 1;
        });

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 26
        );
    }

    private static int spawnAllTargets(CommandSourceStack source) {
        CommandFeedback.withSuppressedSuccess(() -> {
            spawnDefenseTargets(source);
            spawnEnchantTargets(source);
            spawnEffectTargets(source);
            spawnInvulTargets(source);
            spawnEnvironmentalTargets(source);
            return 1;
        });

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 16
        );
    }

    private static int spawnDefenseTargets(CommandSourceStack source) {
        return spawnDefenseTargets(
                source,
                source.getPosition(),
                TestMobSpawnOptions.DEFAULT,
                false
        );
    }

    private static int spawnDefenseTargets(
            CommandSourceStack source,
            Vec3 anchor,
            TestMobSpawnOptions options,
            boolean detailedFeedback
    ) {
        ServerLevel level = source.getLevel();
        Vec3[] positions = {
                anchor.add(2, 0, 0),
                anchor.add(4, 0, 0),
                anchor.add(6, 0, 0),
                anchor.add(8, 0, 0),
                anchor.add(10, 0, 0)
        };

        for (Vec3 position : positions) {
            TestMobFactory.SpawnFailure failure =
                    TestMobFactory.validateSpawnPosition(level, position);
            if (failure != null) {
                return CommandFeedback.fail(
                        source,
                        "command.damagenexus.mob_spawn_failed",
                        "defense",
                        DamageMobCommands.formatPosition(position),
                        Component.translatable(failure.translationKey())
                );
            }
        }

        List<Zombie> spawned = new ArrayList<>(positions.length);

        spawned.add(TestMobFactory.zombie(
                level,
                positions[0],
                "[DN-Test] No Armor",
                ArmorSet.NONE,
                false,
                false,
                options
        ));

        spawned.add(TestMobFactory.zombie(
                level,
                positions[1],
                "[DN-Test] Iron Armor",
                ArmorSet.IRON,
                false,
                false,
                options
        ));

        spawned.add(TestMobFactory.zombie(
                level,
                positions[2],
                "[DN-Test] Diamond Armor",
                ArmorSet.DIAMOND,
                false,
                false,
                options
        ));

        spawned.add(TestMobFactory.zombie(
                level,
                positions[3],
                "[DN-Test] Netherite Prot IV",
                ArmorSet.NETHERITE,
                true,
                false,
                options
        ));

        spawned.add(TestMobFactory.zombie(
                level,
                positions[4],
                "[DN-Test] Resistance I",
                ArmorSet.NONE,
                false,
                true,
                options
        ));

        if (spawned.stream().anyMatch(java.util.Objects::isNull)) {
            spawned.stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(Zombie::discard);
            return CommandFeedback.fail(
                    source,
                    "command.damagenexus.mob_spawn_failed",
                    "defense",
                    DamageMobCommands.formatPosition(anchor),
                    Component.translatable(
                            TestMobFactory.SpawnFailure
                                    .ADD_TO_LEVEL_FAILED
                                    .translationKey()
                    )
            );
        }

        if (detailedFeedback) {
            return CommandFeedback.success(
                    source,
                    "command.damagenexus.defense_spawned",
                    positions.length,
                    DamageMobCommands.formatPosition(anchor),
                    options.immortal()
            );
        }

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 5
        );
    }

    private static int spawnEnchantTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(2, 0, 3),
                "[DN-Test] Undead Target / Smite",
                ArmorSet.NONE,
                false,
                false
        );

        TestMobFactory.cow(
                level,
                pos.add(4, 0, 3),
                "[DN-Test] Cow / Smite Negative"
        );

        TestMobFactory.spider(
                level,
                pos.add(6, 0, 3),
                "[DN-Test] Spider / Bane"
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 3
        );
    }

    private static int spawnEffectTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(2, 0, 6),
                "[DN-Test] Effect Baseline",
                ArmorSet.NONE,
                false,
                false
        );

        Zombie resistance = TestMobFactory.zombie(
                level,
                pos.add(4, 0, 6),
                "[DN-Test] Resistance I Target",
                ArmorSet.NONE,
                false,
                false
        );

        if (resistance != null) {
            resistance.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE,
                    TEN_MINUTES,
                    0,
                    false,
                    true
            ));
        }

        Zombie resistance2 = TestMobFactory.zombie(
                level,
                pos.add(6, 0, 6),
                "[DN-Test] Resistance II Target",
                ArmorSet.NONE,
                false,
                false
        );

        if (resistance2 != null) {
            resistance2.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE,
                    TEN_MINUTES,
                    1,
                    false,
                    true
            ));
        }

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 3
        );
    }

    private static int spawnInvulTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        Zombie fastHit = TestMobFactory.zombie(
                level,
                pos.add(2, 0, 9),
                "[DN-Test] Invul Delta / Fast Hit",
                ArmorSet.NONE,
                false,
                false
        );

        if (fastHit != null) {
            fastHit.invulnerableTime = 10;
        }

        Zombie lowHp = TestMobFactory.zombie(
                level,
                pos.add(4, 0, 9),
                "[DN-Test] Overkill Cap / 5 HP",
                ArmorSet.NONE,
                false,
                false
        );

        if (lowHp != null) {
            lowHp.setHealth(5.0f);
        }

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 2
        );
    }

    private static int spawnEnvironmentalTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(2, 0, 24),
                "[DN-Test] Lava Damage Target",
                ArmorSet.NONE,
                false,
                false
        );

        Zombie burning = TestMobFactory.zombie(
                level,
                pos.add(4, 0, 24),
                "[DN-Test] On Fire Target",
                ArmorSet.NONE,
                false,
                false
        );

        if (burning != null) {
            burning.igniteForSeconds(30.0F);
        }

        TestMobFactory.zombie(
                level,
                pos.add(6, 0, 24),
                "[DN-Test] Burst Hurt Target",
                ArmorSet.NONE,
                false,
                false
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 3
        );
    }

    private static int spawnBridgeTargets(CommandSourceStack source) {
        CommandFeedback.withSuppressedSuccess(() -> {
            spawnProjectileTargets(source);
            spawnMaceTargets(source);
            spawnSpearTargets(source);
            spawnTridentTargets(source);
            spawnMobDifficultyTargets(source);
            return 1;
        });

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 10
        );
    }

    private static int spawnProjectileTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(2, 0, 12),
                "[DN-Test] Projectile Target",
                ArmorSet.NONE,
                false,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(4, 0, 12),
                "[DN-Test] Projectile Target / Armor",
                ArmorSet.IRON,
                false,
                false
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 2
        );
    }

    private static int spawnTridentTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(6, 0, 12),
                Component.translatable(
                        "test.damagenexus.target.trident_negative"
                ),
                ArmorSet.NONE,
                false,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(8, 0, 12),
                Component.translatable(
                        "test.damagenexus.target.trident_armor"
                ),
                ArmorSet.IRON,
                false,
                false
        );

        TestMobFactory.turtle(
                level,
                pos.add(10, 0, 12),
                Component.translatable(
                        "test.damagenexus.target.impaling_positive"
                )
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 3
        );
    }

    private static int spawnMaceTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(2, 0, 15),
                "[DN-Test] Mace Smash Target",
                ArmorSet.NONE,
                false,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(4, 0, 15),
                "[DN-Test] Mace Smash Target / Armor",
                ArmorSet.DIAMOND,
                false,
                false
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 2
        );
    }

    private static int spawnSpearTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(2, 0, 18),
                "[DN-Test] Spear Target",
                ArmorSet.NONE,
                false,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(4, 0, 18),
                "[DN-Test] Spear Target / Armor",
                ArmorSet.IRON,
                false,
                false
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 2
        );
    }

    private static int spawnMobDifficultyTargets(CommandSourceStack source) {
        TestMobFactory.freeZombie(
                source.getLevel(),
                source.getPosition().add(2, 0, 21),
                "[DN-Test] Mob Difficulty Attacker"
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 1
        );
    }
}
