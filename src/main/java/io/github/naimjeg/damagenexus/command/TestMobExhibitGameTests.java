package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.event.DamageSettledEvent;
import io.github.naimjeg.damagenexus.command.test.TestMobFactory;
import io.github.naimjeg.damagenexus.command.test.TestMobPreset;
import io.github.naimjeg.damagenexus.command.test.TestMobSpawnOptions;
import io.github.naimjeg.damagenexus.command.test.TestMobTags;
import io.github.naimjeg.damagenexus.command.test.ResistanceMutation;
import io.github.naimjeg.damagenexus.command.test.TestMobSpawnConfig;
import io.github.naimjeg.damagenexus.command.test.TestResistance;
import io.github.naimjeg.damagenexus.core.gametest.GameTestCodecVerifier;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Real-server coverage for positioned and immortal command test mobs. */
@EventBusSubscriber(modid = DamageNexus.MODID)
final class TestMobExhibitGameTests {

    private static final ResourceKey<Consumer<GameTestHelper>>
            COMMANDS_FUNCTION = functionKey("test_mob_exhibit_commands");
    private static final ResourceKey<Consumer<GameTestHelper>>
            LIFECYCLE_FUNCTION = functionKey("test_mob_exhibit_lifecycle");
    private static final ResourceKey<Consumer<GameTestHelper>>
            ADMIN_FUNCTION = functionKey("test_mob_exhibit_admin");
    private static final ResourceKey<Consumer<GameTestHelper>>
            FORCED_REMOVAL_FUNCTION = functionKey(
            "test_mob_exhibit_forced_removal"
    );
    private static final ResourceKey<Consumer<GameTestHelper>>
            MUTATION_FUNCTION = functionKey("test_mob_mutation");

    private static UUID settlementTarget;
    private static boolean settlementObserved;
    private static float settlementAppliedDamage;

    private TestMobExhibitGameTests() {
    }

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                COMMANDS_FUNCTION.identifier(),
                () -> TestMobExhibitGameTests::positionedCommands
        );
        event.register(
                Registries.TEST_FUNCTION,
                LIFECYCLE_FUNCTION.identifier(),
                () -> TestMobExhibitGameTests::immortalLifecycle
        );
        event.register(
                Registries.TEST_FUNCTION,
                ADMIN_FUNCTION.identifier(),
                () -> TestMobExhibitGameTests::administratorControls
        );
        event.register(
                Registries.TEST_FUNCTION,
                FORCED_REMOVAL_FUNCTION.identifier(),
                () -> TestMobExhibitGameTests::forcedDeathAndRemoval
        );
        event.register(
                Registries.TEST_FUNCTION,
                MUTATION_FUNCTION.identifier(),
                () -> TestMobExhibitGameTests::mutationCommands
        );
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("test_mob_exhibit_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        registerTest(event, environment, COMMANDS_FUNCTION);
        registerTest(event, environment, LIFECYCLE_FUNCTION);
        registerTest(event, environment, ADMIN_FUNCTION);
        registerTest(event, environment, FORCED_REMOVAL_FUNCTION);
        registerTest(event, environment, MUTATION_FUNCTION);
    }

    @SubscribeEvent
    public static void onDamageSettled(DamageSettledEvent event) {
        if (settlementTarget != null
                && event.snapshot().target() != null
                && settlementTarget.equals(
                        event.snapshot().target().getUUID()
                )) {
            settlementObserved = true;
            settlementAppliedDamage = event.snapshot().appliedDamage();
        }
    }

    private static void positionedCommands(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        ServerLevel level = helper.getLevel();
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();

        Vec3 legacySourcePosition = helper.absolutePos(
                new BlockPos(1, 2, 1)
        ).getBottomCenter();
        Mob legacy = executeForSingleMob(
                dispatcher,
                source(level, legacySourcePosition),
                "damagenexus mob baseline",
                level
        );
        assertPosition(
                legacy,
                legacySourcePosition.add(2, 0, 0),
                "legacy baseline did not retain the +2 X offset"
        );
        require(!TestMobTags.isImmortal(legacy),
                "ordinary baseline unexpectedly has the immortal tag");
        legacy.discard();

        Vec3 exact = helper.absolutePos(
                new BlockPos(3, 2, 3)
        ).getBottomCenter();
        Mob absolute = executeForSingleMob(
                dispatcher,
                source(level, helper.absolutePos(
                        new BlockPos(3, 2, 1)
                ).getBottomCenter()),
                String.format(
                        Locale.ROOT,
                        "damagenexus mob baseline at %.1f %.1f %.1f",
                        exact.x,
                        exact.y,
                        exact.z
                ),
                level
        );
        assertPosition(absolute, exact,
                "absolute command did not use the exact feet position");
        require(!TestMobTags.isImmortal(absolute),
                "ordinary positioned target unexpectedly became immortal");
        absolute.discard();

        Vec3 relativeSourcePosition = helper.absolutePos(
                new BlockPos(5, 2, 1)
        ).getBottomCenter();
        Mob relative = executeForSingleMob(
                dispatcher,
                source(level, relativeSourcePosition),
                "damagenexus mob baseline at ~ ~1 ~ immortal",
                level
        );
        assertPosition(
                relative,
                relativeSourcePosition.add(0, 1, 0),
                "relative command did not resolve against the source"
        );
        assertExhibit(relative);
        relative.discard();

        Vec3 defenseAnchor = helper.absolutePos(
                new BlockPos(7, 2, 1)
        ).getBottomCenter();
        int defenseResult = execute(
                dispatcher,
                "damagenexus test targets defense at ~ ~ ~ immortal",
                source(level, defenseAnchor)
        );
        require(defenseResult == 1,
                "positioned immortal defense group failed");
        List<Mob> defense = new ArrayList<>(level.getEntitiesOfClass(
                Zombie.class,
                AABB.ofSize(
                        defenseAnchor.add(6, 0, 0),
                        12.0D,
                        4.0D,
                        4.0D
                ),
                TestMobTags::isImmortal
        ));
        require(defense.size() == 5,
                "defense command did not create exactly five targets");
        for (int index = 0; index < defense.size(); index++) {
            int expectedIndex = index;
            Mob target = defense.stream()
                    .filter(entity -> close(
                            entity.getX(),
                            defenseAnchor.x + 2.0D * (expectedIndex + 1)
                    ))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "defense target spacing changed at index "
                                    + expectedIndex
                    ));
            require(close(target.getY(), defenseAnchor.y)
                            && close(target.getZ(), defenseAnchor.z),
                    "defense target was not anchored to the requested layout");
            assertExhibit(target);
        }
        defense.forEach(Entity::discard);

        Set<UUID> beforeInvalid = testMobUuids(level);
        int invalidResult = execute(
                dispatcher,
                "damagenexus mob baseline at 0 10000 0",
                source(level, helper.absolutePos(
                        new BlockPos(9, 2, 1)
                ).getBottomCenter())
        );
        require(invalidResult == 0,
                "illegal position falsely reported spawn success");
        require(freshTestMobs(level, beforeInvalid).isEmpty(),
                "illegal position left a test entity behind");
        helper.succeed();
    }

    private static void immortalLifecycle(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        ServerLevel level = helper.getLevel();
        Vec3 position = helper.absolutePos(
                new BlockPos(1, 2, 1)
        ).getBottomCenter();
        Cow immortal = (Cow) successfulSpawn(
                level,
                TestMobPreset.BASELINE,
                position,
                TestMobSpawnOptions.IMMORTAL_EXHIBIT
        );
        UUID uuid = immortal.getUUID();
        float maximum = immortal.getMaxHealth();

        require(immortal.hurtServer(
                        level,
                        immortal.damageSources().generic(),
                        3.0F
                ),
                "immortal target rejected nonlethal damage");
        require(immortal.getHealth() < maximum
                        && immortal.getHealth() > 0.0F,
                "nonlethal damage did not reduce exhibit health normally");

        immortal.invulnerableTime = 0;
        int itemDropsBefore = countNearby(
                level,
                ItemEntity.class,
                immortal.position()
        );
        int experienceBefore = countNearby(
                level,
                ExperienceOrb.class,
                immortal.position()
        );
        settlementTarget = uuid;
        settlementObserved = false;
        settlementAppliedDamage = 0.0F;
        boolean accepted = immortal.hurtServer(
                level,
                immortal.damageSources().generic(),
                1000.0F
        );
        settlementTarget = null;

        require(accepted, "immortal target rejected lethal damage");
        require(settlementObserved,
                "lethal hit produced no DamageNexus settlement");
        require(settlementAppliedDamage > maximum,
                "settlement damage was clamped to survivable health: "
                        + settlementAppliedDamage);
        require(!immortal.isRemoved() && immortal.isAlive(),
                "lethal hit removed the immortal target");
        require(immortal.getUUID().equals(uuid),
                "lethal protection replaced the entity UUID");
        require(close(immortal.getHealth(), maximum),
                "lethal protection did not restore maximum health");
        require(itemDropsBefore == countNearby(
                        level,
                        ItemEntity.class,
                        immortal.position()
                ),
                "immortal death protection produced item drops");
        require(experienceBefore == countNearby(
                        level,
                        ExperienceOrb.class,
                        immortal.position()
                ),
                "immortal death protection produced experience");
        immortal.discard();

        Cow ordinary = (Cow) successfulSpawn(
                level,
                TestMobPreset.BASELINE,
                position.add(2, 0, 0),
                TestMobSpawnOptions.DEFAULT
        );
        require(ordinary.hurtServer(
                        level,
                        ordinary.damageSources().generic(),
                        1000.0F
                ),
                "ordinary target rejected lethal damage");
        require(ordinary.isDeadOrDying(),
                "ordinary target death behavior was changed");
        ordinary.discard();
        helper.succeed();
    }

    private static void administratorControls(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        ServerLevel level = helper.getLevel();
        Vec3 position = helper.absolutePos(
                new BlockPos(1, 2, 1)
        ).getBottomCenter();
        Mob mortalized = successfulSpawn(
                level,
                TestMobPreset.BASELINE,
                position,
                TestMobSpawnOptions.IMMORTAL_EXHIBIT
        );
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();
        int result = execute(
                dispatcher,
                "damagenexus mob mortalize @e[tag="
                        + TestMobTags.IMMORTAL
                        + ",distance=..2,limit=1]",
                source(level, position)
        );
        require(result == 1 && !TestMobTags.isImmortal(mortalized),
                "mortalize did not remove exactly one immortal tag");
        require(mortalized.hurtServer(
                        level,
                        mortalized.damageSources().generic(),
                        1000.0F
                ),
                "mortalized target rejected lethal damage");
        require(mortalized.isDeadOrDying(),
                "mortalized target remained immortal");
        mortalized.discard();

        Mob cleanupTarget = successfulSpawn(
                level,
                TestMobPreset.BASELINE,
                position.add(2, 0, 0),
                TestMobSpawnOptions.IMMORTAL_EXHIBIT
        );
        require(DamageCleanupCommands.forceRemoveTestEntity(cleanupTarget),
                "cleanup did not recognize the tagged exhibit");
        require(cleanupTarget.isRemoved(),
                "cleanup did not discard the immortal target immediately");
        helper.succeed();
    }

    /**
     * The vanilla /kill command reaches LivingEntity.kill, which applies the
     * generic-kill source through the full damage/death pipeline. In 26.1.2
     * that source is both NeoForge technical damage and vanilla
     * bypass-invulnerability damage, so it exercises both exclusions from
     * ordinary exhibit protection. Direct discard and cleanup remain removal
     * operations and must never participate in restore semantics.
     */
    private static void forcedDeathAndRemoval(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        ServerLevel level = helper.getLevel();
        Vec3 position = helper.absolutePos(
                new BlockPos(1, 2, 1)
        ).getBottomCenter();
        Mob killTarget = successfulSpawn(
                level,
                TestMobPreset.BASELINE,
                position,
                TestMobSpawnOptions.IMMORTAL_EXHIBIT
        );
        UUID killedUuid = killTarget.getUUID();
        DamageSource genericKill = killTarget.damageSources().genericKill();
        require(genericKill.is(Tags.DamageTypes.IS_TECHNICAL),
                "generic-kill is not tagged as technical damage");
        require(genericKill.is(DamageTypeTags.BYPASSES_INVULNERABILITY),
                "generic-kill does not bypass invulnerability");

        int result = execute(
                level.getServer().getCommands().getDispatcher(),
                "kill @e[tag=" + TestMobTags.IMMORTAL
                        + ",distance=..2,limit=1]",
                source(level, position)
        );
        require(result == 1, "/kill did not select exactly one exhibit");
        require(killTarget.isDeadOrDying(),
                "/kill was canceled by immortal exhibit protection");
        require(!killTarget.entityTags().contains(
                        TestMobTags.PENDING_IMMORTAL_RESTORE),
                "/kill left a pending immortal restore marker");

        pollUntil(
                helper,
                50,
                () -> killTarget.isRemoved()
                        && level.getEntity(killedUuid) == null,
                "/kill target was restored instead of removed",
                () -> {
                    Mob discarded = successfulSpawn(
                            level,
                            TestMobPreset.BASELINE,
                            position.add(2, 0, 0),
                            TestMobSpawnOptions.IMMORTAL_EXHIBIT
                    );
                    UUID discardedUuid = discarded.getUUID();
                    discarded.discard();
                    require(discarded.isRemoved(),
                            "discard did not remove the exhibit immediately");

                    Mob cleanupTarget = successfulSpawn(
                            level,
                            TestMobPreset.BASELINE,
                            position.add(4, 0, 0),
                            TestMobSpawnOptions.IMMORTAL_EXHIBIT
                    );
                    UUID cleanupUuid = cleanupTarget.getUUID();
                    require(DamageCleanupCommands.forceRemoveTestEntity(
                                    cleanupTarget),
                            "cleanup did not recognize the immortal exhibit");
                    require(cleanupTarget.isRemoved(),
                            "cleanup did not remove the exhibit immediately");

                    helper.runAfterDelay(2, () -> {
                        require(level.getEntity(discardedUuid) == null,
                                "discarded exhibit reappeared on a later tick");
                        require(level.getEntity(cleanupUuid) == null,
                                "cleaned exhibit reappeared on a later tick");
                        helper.succeed();
                    });
                }
        );
    }

    /**
     * Real-server coverage for the pre-spawn {@code mutation resistance}
     * command grammar and the {@link TestMobFactory} mutation pipeline.
     */
    private static void mutationCommands(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        ServerLevel level = helper.getLevel();
        Vec3 position = helper.absolutePos(
                new BlockPos(1, 2, 1)
        ).getBottomCenter();

        // 16.1 direct factory mutation must SET the base value, not add.
        TestMobFactory.SpawnResult direct = TestMobFactory.spawnPreset(
                level,
                TestMobPreset.BASELINE,
                position,
                new TestMobSpawnConfig(
                        TestMobSpawnOptions.DEFAULT,
                        List.of(new ResistanceMutation(
                                TestResistance.FIRE,
                                50.0D
                        ))
                )
        );
        require(direct.succeeded(),
                "direct mutation spawn failed: " + direct.failure());
        Mob mutated = direct.entity();
        require(mutated.getAttribute(ModAttributes.RESISTANCE_FIRE)
                        .getBaseValue() == 50.0D,
                "direct mutation did not set fire resistance base to 50");
        require(mutated.getAttribute(ModAttributes.RESISTANCE_PHYSICAL)
                        .getBaseValue() == 0.0D,
                "direct mutation altered an unrelated resistance");
        mutated.discard();

        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();

        // 16.2 default-position command mutation.
        Mob defaultMob = executeForSingleMob(
                dispatcher,
                source(level, position.add(2, 0, 0)),
                "damagenexus mob zombie mutation resistance fire 50",
                level
        );
        require(defaultMob.getAttribute(ModAttributes.RESISTANCE_FIRE)
                        .getBaseValue() == 50.0D,
                "default-position command mutation did not set fire base to 50");
        defaultMob.discard();

        // 16.3 explicit at-position command mutation.
        Vec3 atPosition = helper.absolutePos(
                new BlockPos(3, 2, 3)
        ).getBottomCenter();
        Mob atMob = executeForSingleMob(
                dispatcher,
                source(level, position.add(4, 0, 0)),
                String.format(
                        Locale.ROOT,
                        "damagenexus mob zombie at %.1f %.1f %.1f "
                                + "mutation resistance physical 100",
                        atPosition.x,
                        atPosition.y,
                        atPosition.z
                ),
                level
        );
        assertPosition(atMob, atPosition,
                "at-position mutation did not use the exact position");
        require(atMob.getAttribute(ModAttributes.RESISTANCE_PHYSICAL)
                        .getBaseValue() == 100.0D,
                "at-position mutation did not set physical base to 100");
        atMob.discard();

        // 16.4 at + immortal + mutation must preserve the exhibit semantics.
        Vec3 exhibitPosition = helper.absolutePos(
                new BlockPos(5, 2, 5)
        ).getBottomCenter();
        Mob exhibit = executeForSingleMob(
                dispatcher,
                source(level, position.add(6, 0, 0)),
                String.format(
                        Locale.ROOT,
                        "damagenexus mob zombie at %.1f %.1f %.1f "
                                + "immortal mutation resistance magic 75",
                        exhibitPosition.x,
                        exhibitPosition.y,
                        exhibitPosition.z
                ),
                level
        );
        assertPosition(exhibit, exhibitPosition,
                "immortal mutation did not use the exact position");
        assertExhibit(exhibit);
        require(exhibit.getAttribute(ModAttributes.RESISTANCE_MAGIC)
                        .getBaseValue() == 75.0D,
                "immortal mutation did not set magic base to 75");
        exhibit.discard();

        // 16.5 ordinary command must not pollute the global resistance defaults.
        Mob plain = executeForSingleMob(
                dispatcher,
                source(level, position.add(8, 0, 0)),
                "damagenexus mob zombie",
                level
        );
        for (TestResistance testResistance : TestResistance.values()) {
            require(plain.getAttribute(testResistance.attribute())
                            .getBaseValue() == 0.0D,
                    "plain zombie resistance was polluted: "
                            + testResistance.commandName());
        }
        plain.discard();

        // 16.6 out-of-range value must fail to parse and leave no test mob.
        Set<UUID> beforeInvalid = testMobUuids(level);
        int invalidResult = executeParseFailure(
                dispatcher,
                "damagenexus mob zombie mutation resistance fire 20000",
                source(level, position.add(10, 0, 0))
        );
        require(invalidResult < 0,
                "out-of-range mutation falsely reported spawn success");
        require(freshTestMobs(level, beforeInvalid).isEmpty(),
                "out-of-range mutation left a test entity behind");

        helper.succeed();
    }

    private static int executeParseFailure(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String command,
            CommandSourceStack source
    ) {
        try {
            return dispatcher.execute(command, source);
        } catch (CommandSyntaxException exception) {
            return -1;
        }
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher() {
        CommandDispatcher<CommandSourceStack> dispatcher =
                new CommandDispatcher<>();
        var root = Commands.literal("damagenexus");
        DamageMobCommands.register(root);
        DamageTestCommands.register(root);
        dispatcher.register(root);
        return dispatcher;
    }

    private static CommandSourceStack source(
            ServerLevel level,
            Vec3 position
    ) {
        return level.getServer().createCommandSourceStack()
                .withLevel(level)
                .withPosition(position)
                .withPermission(PermissionSet.ALL_PERMISSIONS)
                .withSuppressedOutput();
    }

    private static Mob executeForSingleMob(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source,
            String command,
            ServerLevel level
    ) {
        Set<UUID> before = testMobUuids(level);
        require(execute(dispatcher, command, source) == 1,
                "command failed: " + command);
        List<Mob> fresh = freshTestMobs(level, before);
        require(fresh.size() == 1,
                "command did not create exactly one target: " + command);
        return fresh.getFirst();
    }

    private static int execute(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String command,
            CommandSourceStack source
    ) {
        try {
            return dispatcher.execute(command, source);
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("command failed: " + command, exception);
        }
    }

    private static Mob successfulSpawn(
            ServerLevel level,
            TestMobPreset preset,
            Vec3 position,
            TestMobSpawnOptions options
    ) {
        TestMobFactory.SpawnResult result = TestMobFactory.spawnPreset(
                level,
                preset,
                position,
                options
        );
        require(result.succeeded(),
                "factory spawn failed: " + result.failure());
        return result.entity();
    }

    private static Set<UUID> testMobUuids(ServerLevel level) {
        Set<UUID> result = new HashSet<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof Mob mob
                    && TestMobTags.isTestEntity(mob)) {
                result.add(mob.getUUID());
            }
        }
        return result;
    }

    private static List<Mob> freshTestMobs(
            ServerLevel level,
            Set<UUID> before
    ) {
        List<Mob> result = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof Mob mob
                    && TestMobTags.isTestEntity(mob)
                    && !before.contains(mob.getUUID())) {
                result.add(mob);
            }
        }
        return result;
    }

    private static void assertExhibit(Mob mob) {
        require(TestMobTags.isImmortal(mob),
                "exhibit is missing the immortal tag");
        require(mob.isPersistenceRequired(),
                "exhibit is not persistent");
        require(mob.isNoAi(), "exhibit AI was not disabled");
        require(!mob.isInvulnerable(),
                "exhibit used vanilla invulnerability");
        require(mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)
                        >= 1.0D,
                "exhibit did not receive knockback resistance");
    }

    private static void assertPosition(
            Entity entity,
            Vec3 expected,
            String message
    ) {
        require(close(entity.getX(), expected.x)
                        && close(entity.getY(), expected.y)
                        && close(entity.getZ(), expected.z),
                message + ": " + entity.position() + " != " + expected);
    }

    private static <T extends Entity> int countNearby(
            ServerLevel level,
            Class<T> type,
            Vec3 center
    ) {
        return level.getEntitiesOfClass(
                type,
                AABB.ofSize(center, 8.0D, 8.0D, 8.0D)
        ).size();
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) <= 1.0E-6D;
    }

    private static void pollUntil(
            GameTestHelper helper,
            int attempts,
            BooleanSupplier condition,
            String failureMessage,
            Runnable onSuccess
    ) {
        if (condition.getAsBoolean()) {
            onSuccess.run();
            return;
        }
        if (attempts <= 0) {
            helper.fail(failureMessage);
            return;
        }
        helper.runAfterDelay(1, () -> pollUntil(
                helper,
                attempts - 1,
                condition,
                failureMessage,
                onSuccess
        ));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            ResourceKey<Consumer<GameTestHelper>> function
    ) {
        event.registerTest(
                function.identifier(),
                new FunctionGameTestInstance(
                        function,
                        new TestData<>(
                                environment,
                                Identifier.withDefaultNamespace("empty"),
                                100,
                                0,
                                true,
                                Rotation.NONE
                        )
                )
        );
    }

    private static ResourceKey<Consumer<GameTestHelper>> functionKey(
            String path
    ) {
        return ResourceKey.create(Registries.TEST_FUNCTION, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DamageNexus.MODID, path);
    }
}
