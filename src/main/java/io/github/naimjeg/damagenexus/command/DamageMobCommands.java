package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory.ArmorSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

public final class DamageMobCommands {

    private DamageMobCommands() {
    }

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(Commands.literal("mob")
                .requires(DamageCommandSecurity.adminPermission())
                .then(Commands.literal("baseline")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnBaselineMob(ctx.getSource())
                        )))
                .then(Commands.literal("zombie")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnSingleZombie(ctx.getSource())
                        )))
                .then(Commands.literal("cow")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnSingleCow(ctx.getSource())
                        )))
                .then(Commands.literal("spider")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnSingleSpider(ctx.getSource())
                        )))
                .then(Commands.literal("iron")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnArmoredTarget(
                                        ctx.getSource(),
                                        ArmorSet.IRON,
                                        false,
                                        "[DN-Test] Iron Armor"
                                )
                        )))
                .then(Commands.literal("diamond")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnArmoredTarget(
                                        ctx.getSource(),
                                        ArmorSet.DIAMOND,
                                        false,
                                        "[DN-Test] Diamond Armor"
                                )
                        )))
                .then(Commands.literal("netherite_prot")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnArmoredTarget(
                                        ctx.getSource(),
                                        ArmorSet.NETHERITE,
                                        true,
                                        "[DN-Test] Netherite Prot IV"
                                )
                        )))
                .then(Commands.literal("low_hp")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnLowHpTarget(ctx.getSource())
                        )))
                .then(Commands.literal("invul")
                        .executes(ctx -> guarded(
                                ctx.getSource(),
                                () -> spawnInvulTarget(ctx.getSource())
                        ))));
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

    private static int spawnBaselineMob(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        TestMobFactory.cow(
                level,
                pos.add(2, 0, 0),
                "[DN-Test] Baseline / No Armor"
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 1
        );
    }

    private static int spawnSingleZombie(CommandSourceStack source) {
        TestMobFactory.zombie(
                source.getLevel(),
                source.getPosition().add(2, 0, 0),
                "[DN-Test] Zombie",
                ArmorSet.NONE,
                false,
                false
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 1
        );
    }

    private static int spawnSingleCow(CommandSourceStack source) {
        TestMobFactory.cow(
                source.getLevel(),
                source.getPosition().add(2, 0, 0),
                "[DN-Test] Cow"
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 1
        );
    }

    private static int spawnSingleSpider(CommandSourceStack source) {
        TestMobFactory.spider(
                source.getLevel(),
                source.getPosition().add(2, 0, 0),
                "[DN-Test] Spider"
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 1
        );
    }

    private static int spawnArmoredTarget(
            CommandSourceStack source,
            ArmorSet armorSet,
            boolean protectionIv,
            String name
    ) {
        TestMobFactory.zombie(
                source.getLevel(),
                source.getPosition().add(2, 0, 0),
                name,
                armorSet,
                protectionIv,
                false
        );

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 1
        );
    }

    private static int spawnLowHpTarget(CommandSourceStack source) {
        Zombie target = TestMobFactory.zombie(
                source.getLevel(),
                source.getPosition().add(2, 0, 0),
                "[DN-Test] Overkill Cap / 5 HP",
                ArmorSet.NONE,
                false,
                false
        );

        if (target != null) {
            target.setHealth(5.0f);
        }

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 1
        );
    }

    private static int spawnInvulTarget(CommandSourceStack source) {
        Zombie target = TestMobFactory.zombie(
                source.getLevel(),
                source.getPosition().add(2, 0, 0),
                "[DN-Test] Invul Delta / Fast Hit",
                ArmorSet.NONE,
                false,
                false
        );

        if (target != null) {
            target.invulnerableTime = 10;
        }

        return CommandFeedback.success(
                source,
                "command.damagenexus.targets_created", 1
        );
    }
}
