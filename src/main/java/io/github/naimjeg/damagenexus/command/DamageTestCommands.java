package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory.ArmorSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

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
                                )))
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
        spawnAllTargets(source);
        spawnBridgeTargets(source);

        return CommandFeedback.success(
                source,
                "all DamageNexus test targets generated."
        );
    }

    private static int spawnAllTargets(CommandSourceStack source) {
        spawnDefenseTargets(source);
        spawnEnchantTargets(source);
        spawnEffectTargets(source);
        spawnInvulTargets(source);
        spawnEnvironmentalTargets(source);

        return CommandFeedback.success(
                source,
                "all basic test targets generated."
        );
    }

    private static int spawnDefenseTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(2, 0, 0),
                "[DN-Test] No Armor",
                ArmorSet.NONE,
                false,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(4, 0, 0),
                "[DN-Test] Iron Armor",
                ArmorSet.IRON,
                false,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(6, 0, 0),
                "[DN-Test] Diamond Armor",
                ArmorSet.DIAMOND,
                false,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(8, 0, 0),
                "[DN-Test] Netherite Prot IV",
                ArmorSet.NETHERITE,
                true,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(10, 0, 0),
                "[DN-Test] Resistance I",
                ArmorSet.NONE,
                false,
                true
        );

        return CommandFeedback.success(
                source,
                "defense targets generated."
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
                "enchantment bridge targets generated."
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
                "mob effect targets generated."
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
                "post classification targets generated."
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
                "environmental damage targets generated."
        );
    }

    private static int spawnBridgeTargets(CommandSourceStack source) {
        spawnProjectileTargets(source);
        spawnMaceTargets(source);
        spawnSpearTargets(source);
        spawnTridentTargets(source);
        spawnMobDifficultyTargets(source);

        return CommandFeedback.success(
                source,
                "bridge test targets generated."
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
                "projectile bridge targets generated."
        );
    }

    private static int spawnTridentTargets(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.zombie(
                level,
                pos.add(6, 0, 12),
                "[DN-Test] Trident Projectile Target",
                ArmorSet.NONE,
                false,
                false
        );

        TestMobFactory.zombie(
                level,
                pos.add(8, 0, 12),
                "[DN-Test] Trident Projectile Target / Armor",
                ArmorSet.IRON,
                false,
                false
        );

        return CommandFeedback.success(
                source,
                "trident projectile targets generated."
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
                "mace bridge targets generated."
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
                "spear bridge targets generated."
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
                "mob difficulty attacker generated."
        );
    }
}
