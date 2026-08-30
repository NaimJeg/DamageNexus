package io.github.naimjeg.damagenexus.entity;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.block.entity.DamageDummyBlockEntity;
import io.github.naimjeg.damagenexus.core.gametest.GameTestCodecVerifier;
import io.github.naimjeg.damagenexus.menu.DamageDummyMenu;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import io.github.naimjeg.damagenexus.registry.ModBlocks;
import io.github.naimjeg.damagenexus.registry.ModEntityTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Integration coverage for snapshots and atomic, sanitized attribute batches. */
@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageDummyAttributeGameTests {

    private static final ResourceKey<Consumer<GameTestHelper>> SERVICE_FUNCTION =
            functionKey("damage_dummy_attribute_service");
    private static final ResourceKey<Consumer<GameTestHelper>> PEDESTAL_FUNCTION =
            functionKey("damage_dummy_attribute_pedestal");
    private static final Identifier MODIFIER_ID = id("snapshot_test_modifier");
    private static final Identifier MAX_HEALTH_ID =
            Identifier.withDefaultNamespace("max_health");

    private DamageDummyAttributeGameTests() {
    }

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                SERVICE_FUNCTION.identifier(),
                () -> DamageDummyAttributeGameTests::serviceContract
        );
        event.register(
                Registries.TEST_FUNCTION,
                PEDESTAL_FUNCTION.identifier(),
                () -> DamageDummyAttributeGameTests::pedestalContract
        );
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("damage_dummy_attribute_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        event.registerTest(
                id("damage_dummy_attribute_service"),
                new FunctionGameTestInstance(
                        SERVICE_FUNCTION,
                        testData(environment, 40)
                )
        );
        event.registerTest(
                id("damage_dummy_attribute_pedestal"),
                new FunctionGameTestInstance(
                        PEDESTAL_FUNCTION,
                        testData(environment, 120)
                )
        );
    }

    private static void serviceContract(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageDummyEntity dummy = helper.spawn(
                ModEntityTypes.DAMAGE_DUMMY.get(),
                new BlockPos(1, 2, 1)
        );
        AttributeInstance maxHealth = dummy.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            throw new AssertionError("dummy is missing max health");
        }
        double expectedMaxHealth = Attributes.MAX_HEALTH.value()
                .sanitizeValue(Double.MAX_VALUE);
        maxHealth.addTransientModifier(new AttributeModifier(
                MODIFIER_ID,
                5.0D,
                AttributeModifier.Operation.ADD_VALUE
        ));

        DamageDummyAttributeSnapshot snapshot =
                DamageDummyAttributeService.snapshot(BlockPos.ZERO, dummy);
        List<DamageDummyAttributes.AvailableAttribute> available =
                DamageDummyAttributes.availableAttributes(dummy);
        if (snapshot.attributes().size() != available.size()) {
            throw new AssertionError("snapshot count differs from catalog");
        }
        for (int index = 0; index < available.size(); index++) {
            if (!snapshot.attributes().get(index).id()
                    .equals(available.get(index).id())) {
                throw new AssertionError("snapshot order differs from catalog");
            }
        }
        DamageDummyAttributeView healthView = snapshot.attributes().stream()
                .filter(view -> view.id().equals(MAX_HEALTH_ID))
                .findFirst()
                .orElseThrow();
        if (healthView.baseValue() == healthView.effectiveValue()
                || healthView.effectiveValue() != 25.0D) {
            throw new AssertionError("snapshot lost base/effective distinction");
        }
        maxHealth.removeModifier(MODIFIER_ID);

        Identifier critId = ModAttributes.CRIT_CHANCE.getId();
        assertApplied(
                dummy,
                List.of(
                        new DamageDummyAttributeEdit(MAX_HEALTH_ID, 40.0D),
                        new DamageDummyAttributeEdit(critId, 0.5D)
                )
        );
        if (maxHealth.getBaseValue() != 40.0D
                || dummy.getAttribute(ModAttributes.CRIT_CHANCE)
                .getBaseValue() != 0.5D) {
            throw new AssertionError("multi-attribute batch was not applied");
        }

        assertApplied(
                dummy,
                List.of(new DamageDummyAttributeEdit(
                        MAX_HEALTH_ID,
                        5000.0D
                ))
        );
        if (maxHealth.getBaseValue() != expectedMaxHealth) {
            throw new AssertionError("sanitizeValue did not clamp base value");
        }

        verifyResistanceRangeAndDummySanitization(dummy);

        assertRejected(dummy, List.of(new DamageDummyAttributeEdit(
                MAX_HEALTH_ID,
                Double.NaN
        )));
        assertRejected(dummy, List.of(new DamageDummyAttributeEdit(
                MAX_HEALTH_ID,
                Double.POSITIVE_INFINITY
        )));
        assertRejected(dummy, List.of(new DamageDummyAttributeEdit(
                id("unknown_attribute"),
                1.0D
        )));
        assertRejected(dummy, List.of(
                new DamageDummyAttributeEdit(MAX_HEALTH_ID, 20.0D),
                new DamageDummyAttributeEdit(MAX_HEALTH_ID, 30.0D)
        ));

        List<DamageDummyAttributeEdit> oversized = new ArrayList<>();
        for (int index = 0;
                index <= DamageDummyAttributeProtocol.MAX_ATTRIBUTES;
                index++) {
            oversized.add(new DamageDummyAttributeEdit(MAX_HEALTH_ID, index));
        }
        assertRejected(dummy, oversized);

        maxHealth.setBaseValue(31.0D);
        assertRejected(dummy, List.of(
                new DamageDummyAttributeEdit(MAX_HEALTH_ID, 70.0D),
                new DamageDummyAttributeEdit(id("unknown_attribute"), 1.0D)
        ));
        if (maxHealth.getBaseValue() != 31.0D) {
            throw new AssertionError("invalid batch partially modified state");
        }
        helper.succeed();
    }

    private static void verifyResistanceRangeAndDummySanitization(
            DamageDummyEntity dummy
    ) {
        List<Holder<Attribute>> resistances = List.of(
                ModAttributes.RESISTANCE_PHYSICAL,
                ModAttributes.RESISTANCE_FIRE,
                ModAttributes.RESISTANCE_COLD,
                ModAttributes.RESISTANCE_LIGHTNING,
                ModAttributes.RESISTANCE_MAGIC,
                ModAttributes.RESISTANCE_WITHER,
                ModAttributes.RESISTANCE_POISON,
                ModAttributes.RESISTANCE_MELEE,
                ModAttributes.RESISTANCE_PROJECTILE,
                ModAttributes.RESISTANCE_KINETIC
        );

        for (Holder<Attribute> resistance : resistances) {
            Attribute attribute = resistance.value();
            if (attribute.getDefaultValue() != 0.0D
                    || attribute.sanitizeValue(-Double.MAX_VALUE)
                    != ModAttributes.RESISTANCE_RATING_MIN
                    || attribute.sanitizeValue(Double.MAX_VALUE)
                    != ModAttributes.RESISTANCE_RATING_MAX) {
                throw new AssertionError(
                        "DamageNexus resistance range is not -65535..65535: "
                                + resistance.getKey().identifier()
                );
            }
        }

        Identifier lightningId = ModAttributes.RESISTANCE_LIGHTNING.getId();
        AttributeInstance lightning = dummy.getAttribute(
                ModAttributes.RESISTANCE_LIGHTNING
        );
        if (lightning == null) {
            throw new AssertionError("dummy is missing lightning resistance");
        }

        assertResistanceEdit(
                dummy,
                lightning,
                lightningId,
                ModAttributes.RESISTANCE_RATING_MIN,
                ModAttributes.RESISTANCE_RATING_MIN
        );
        assertResistanceEdit(
                dummy,
                lightning,
                lightningId,
                ModAttributes.RESISTANCE_RATING_MIN - 1.0D,
                ModAttributes.RESISTANCE_RATING_MIN
        );
        assertResistanceEdit(
                dummy,
                lightning,
                lightningId,
                ModAttributes.RESISTANCE_RATING_MAX,
                ModAttributes.RESISTANCE_RATING_MAX
        );
        assertResistanceEdit(
                dummy,
                lightning,
                lightningId,
                ModAttributes.RESISTANCE_RATING_MAX + 1.0D,
                ModAttributes.RESISTANCE_RATING_MAX
        );
    }

    private static void assertResistanceEdit(
            DamageDummyEntity dummy,
            AttributeInstance instance,
            Identifier attributeId,
            double requested,
            double expected
    ) {
        assertApplied(dummy, List.of(
                new DamageDummyAttributeEdit(attributeId, requested)
        ));
        if (instance.getBaseValue() != expected) {
            throw new AssertionError(
                    "damage dummy did not use the attribute range: requested="
                            + requested + " actual=" + instance.getBaseValue()
            );
        }
    }

    private static void pedestalContract(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        BlockPos relative = new BlockPos(1, 2, 1);
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, ModBlocks.DAMAGE_DUMMY.get());
        helper.runAfterDelay(45, () -> {
            DamageDummyBlockEntity blockEntity = helper.getBlockEntity(
                    relative,
                    DamageDummyBlockEntity.class
            );
            DamageDummyAttributeSnapshot snapshot =
                    DamageDummyAttributeService.snapshot(
                            helper.getLevel(),
                            blockEntity
                    );
            if (!snapshot.available() || snapshot.attributes().isEmpty()) {
                throw new AssertionError("pedestal snapshot is unavailable");
            }
            var player = helper.makeMockPlayer(GameType.CREATIVE);
            player.setPos(absolute.getCenter());
            DamageDummyMenu menu = new DamageDummyMenu(
                    37,
                    player.getInventory(),
                    blockEntity
            );
            if (!DamageDummyAttributeService.requestMatchesMenu(
                    menu,
                    37,
                    absolute
            ) || DamageDummyAttributeService.requestMatchesMenu(
                    menu,
                    38,
                    absolute
            ) || DamageDummyAttributeService.requestMatchesMenu(
                    menu,
                    37,
                    absolute.above()
            )) {
                throw new AssertionError("menu request identity validation failed");
            }
            blockEntity.resolveManagedDummy(helper.getLevel())
                    .orElseThrow()
                    .discard();
            if (DamageDummyAttributeService.snapshot(
                    helper.getLevel(),
                    blockEntity
            ).available()) {
                throw new AssertionError("missing dummy snapshot stayed available");
            }
            helper.succeed();
        });
    }

    private static void assertApplied(
            DamageDummyEntity dummy,
            List<DamageDummyAttributeEdit> edits
    ) {
        if (DamageDummyAttributeService.validateAndApply(dummy, edits)
                != DamageDummyAttributeService.ApplyResult.APPLIED) {
            throw new AssertionError("valid batch was rejected");
        }
    }

    private static void assertRejected(
            DamageDummyEntity dummy,
            List<DamageDummyAttributeEdit> edits
    ) {
        if (DamageDummyAttributeService.validateAndApply(dummy, edits)
                != DamageDummyAttributeService.ApplyResult.INVALID_BATCH) {
            throw new AssertionError("invalid batch was accepted");
        }
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(
            Holder<TestEnvironmentDefinition<?>> environment,
            int maxTicks
    ) {
        return new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                maxTicks,
                0,
                true,
                Rotation.NONE
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
