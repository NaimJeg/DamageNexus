package io.github.naimjeg.damagenexus.command;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.DamageNexusApi;
import io.github.naimjeg.damagenexus.api.damage.DamageRequest;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.damage.DamageResult;
import io.github.naimjeg.damagenexus.api.damage.DamageSourceDescriptor;
import io.github.naimjeg.damagenexus.api.damage.DamageSubmissionStatus;
import io.github.naimjeg.damagenexus.command.test.TestItemFactory;
import io.github.naimjeg.damagenexus.core.gametest.GameTestCodecVerifier;
import io.github.naimjeg.damagenexus.core.gametest.GameTestServerPlayerFactory;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Launch-only pipeline coverage for the conditional high-health test item.
 * Verifies through the real DamageNexus pipeline, not just rule structure:
 * final/post multiplier +25% (1.25x) applies strictly above 80% health, and
 * does not apply at exactly 80% or below.
 */
@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageConditionalItemGameTests {

    private static final float EPSILON = 0.001f;
    private static final float BASE_DAMAGE = 4.0f;
    private static final ResourceKey<Consumer<GameTestHelper>>
            CONDITIONAL_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("conditional_high_health_item")
    );

    private DamageConditionalItemGameTests() {
    }

    @SubscribeEvent
    public static void registerTestFunction(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                CONDITIONAL_FUNCTION.identifier(),
                () -> DamageConditionalItemGameTests::conditionalHighHealthItem
        );
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("conditional_item_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(
                        environment,
                        Identifier.withDefaultNamespace("empty"),
                        20,
                        0,
                        true,
                        Rotation.NONE
                );

        event.registerTest(
                id("conditional_high_health_item"),
                new FunctionGameTestInstance(
                        CONDITIONAL_FUNCTION,
                        data
                )
        );
    }

    private static void conditionalHighHealthItem(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer attacker = GameTestServerPlayerFactory.create(helper);

        try {
            // A. Target health strictly above 80%: +25% final multiplier.
            assertBoundary(helper, attacker, 1.0f, true);
            // B. Target health exactly 80%: no multiplier.
            assertBoundary(helper, attacker, 0.80f, false);
            // C. Target health below 80% (79%): no multiplier.
            assertBoundary(helper, attacker, 0.79f, false);

            DamageNexus.LOGGER.info(
                    "[DamageNexus] Conditional high-health item GameTest passed"
            );
            helper.succeed();
        } finally {
            level.getServer().getPlayerList()
                    .deop(attacker.nameAndId());
        }
    }

    private static void assertBoundary(
            GameTestHelper helper,
            ServerPlayer attacker,
            float healthFraction,
            boolean shouldMultiply
    ) {
        float control = resolvedWithoutItem(
                helper,
                attacker,
                healthFraction
        );
        float boosted = resolvedWithItem(
                helper,
                attacker,
                healthFraction
        );

        requireClose(
                control,
                BASE_DAMAGE,
                "control resolved damage drifted from base " + BASE_DAMAGE
        );

        if (shouldMultiply) {
            requireClose(
                    boosted,
                    BASE_DAMAGE * 1.25f,
                    ">80% expected 4 * 1.25 = 5, got " + boosted
            );
            requireClose(
                    boosted,
                    control * 1.25f,
                    ">80% expected a 1.25x post multiplier over the control"
            );
        } else {
            requireClose(
                    boosted,
                    control,
                    "80%/below must not apply the conditional multiplier"
            );
            requireClose(
                    boosted,
                    BASE_DAMAGE,
                    "80%/below expected base damage unchanged, got " + boosted
            );
        }
    }

    private static float resolvedWithoutItem(
            GameTestHelper helper,
            ServerPlayer attacker,
            float healthFraction
    ) {
        attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        return submitAtHealth(helper, attacker, healthFraction);
    }

    private static float resolvedWithItem(
            GameTestHelper helper,
            ServerPlayer attacker,
            float healthFraction
    ) {
        attacker.setItemSlot(
                EquipmentSlot.MAINHAND,
                TestItemFactory.targetHighHealthBonusSword()
        );
        return submitAtHealth(helper, attacker, healthFraction);
    }

    private static float submitAtHealth(
            GameTestHelper helper,
            ServerPlayer attacker,
            float healthFraction
    ) {
        LivingEntity target = helper.spawnWithNoFreeWill(
                EntityType.ZOMBIE,
                new BlockPos(1, 2, 1)
        );
        try {
            target.setHealth(target.getMaxHealth() * healthFraction);
            DamageResult result = DamageNexusApi.submitDamage(
                    DamageRequest.builder(
                                    helper.getLevel(),
                                    target,
                                    DamageSourceDescriptor.of(
                                            DamageTypes.PLAYER_ATTACK
                                    ),
                                    BASE_DAMAGE
                            )
                            .logicalAttacker(attacker)
                            .equipmentOwner(attacker)
                            .kind(DamageRequestKind.PRIMARY)
                            .actionId(id("conditional_item_test"))
                            .build()
            );

            if (result.status() != DamageSubmissionStatus.APPLIED) {
                throw new AssertionError(
                        "conditional request was not applied: "
                                + result.status()
                );
            }
            return result.settlement().orElseThrow().resolvedDamage();
        } finally {
            target.discard();
            attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    private static void requireClose(
            float actual,
            float expected,
            String message
    ) {
        if (Math.abs(actual - expected) > EPSILON) {
            throw new AssertionError(message);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DamageNexus.MODID, path);
    }
}
