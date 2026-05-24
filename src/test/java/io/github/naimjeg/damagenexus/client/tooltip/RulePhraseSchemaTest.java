package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.client.phrase.*;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddChannelPostMultiplierOperation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static io.github.naimjeg.damagenexus.api.client.phrase.DamageNexusRulePhrases.*;
import static org.junit.jupiter.api.Assertions.*;

class RulePhraseSchemaTest {
    @Test
    void everySchemaValidatesRequiredSlotsTypesAndVariants() {
        RulePhraseRegistry registry = registry(false);
        for (RulePhraseSchema schema : registry.schemas()) {
            PhraseArguments valid = validArguments(schema);
            for (PhraseVariant variant : schema.variants()) {
                assertDoesNotThrow(() -> schema.create(variant, valid),
                        schema.type().id().toString());
            }
            if (!schema.slots().isEmpty()) {
                assertThrows(IllegalArgumentException.class, () ->
                        schema.create(schema.variants().iterator().next(), PhraseArguments.EMPTY),
                        schema.type().id().toString());
            }
            for (PhraseVariant variant : PhraseVariant.values()) {
                if (!schema.variants().contains(variant)) {
                    assertThrows(IllegalArgumentException.class, () ->
                            schema.create(variant, valid));
                }
            }
        }
    }

    @Test
    void missingUnknownDuplicateAndWrongTypeArgumentsAreRejected() {
        RulePhraseSchema schema = new RulePhraseSchema(
                CHANGE_CHANNEL_DAMAGE,
                Set.of(PhraseVariant.INCREASE),
                List.of(CHANNEL, PERCENT)
        );
        PhraseSlot<NumberValue> unknown = PhraseSlot.required("unknown", NumberValue.class);
        assertThrows(IllegalArgumentException.class, () -> schema.create(
                PhraseVariant.INCREASE,
                PhraseArguments.builder().put(CHANNEL, new ChannelValue(id("fire"))).build()
        ));
        assertThrows(IllegalArgumentException.class, () -> schema.create(
                PhraseVariant.INCREASE,
                new PhraseArguments(List.of(
                        new PhraseArgument(CHANNEL, new ChannelValue(id("fire"))),
                        new PhraseArgument(PERCENT, new PercentValue(0.2)),
                        new PhraseArgument(unknown, new NumberValue(1))
                ))
        ));
        assertThrows(IllegalArgumentException.class, () -> schema.create(
                PhraseVariant.INCREASE,
                new PhraseArguments(List.of(
                        new PhraseArgument(CHANNEL, new ChannelValue(id("fire"))),
                        new PhraseArgument(CHANNEL, new ChannelValue(id("cold"))),
                        new PhraseArgument(PERCENT, new PercentValue(0.2))
                ))
        ));
        assertThrows(IllegalArgumentException.class, () -> schema.create(
                PhraseVariant.INCREASE,
                new PhraseArguments(List.of(
                        new PhraseArgument(CHANNEL, new NumberValue(1)),
                        new PhraseArgument(PERCENT, new PercentValue(0.2))
                ))
        ));
    }

    @Test
    void registryRejectsDuplicatesAndMutationsAfterFreeze() {
        RulePhraseRegistry registry = registry(false);
        assertThrows(IllegalStateException.class, () -> registry.registerSchema(
                new RulePhraseSchema(ALWAYS, Set.of(PhraseVariant.DEFAULT), List.of())
        ));
        registry.freeze();
        assertTrue(registry.isFrozen());
        assertThrows(IllegalStateException.class, () -> registry.registerSchema(
                new RulePhraseSchema(RulePhraseType.of("example", "late"),
                        Set.of(PhraseVariant.DEFAULT), List.of())
        ));
    }

    @Test
    void registryRejectsDuplicateProviderIds() {
        RulePhraseRegistry registry = registry(false);
        assertThrows(IllegalStateException.class, () -> registry.registerOperation(
                DamageNexusOperationIds.ADD_CHANNEL_POST_MULTIPLIER,
                AddChannelPostMultiplierOperation.class,
                (operation, phrases) -> phrases.create(
                        CHANGE_GLOBAL_DAMAGE,
                        PhraseVariant.INCREASE,
                        PhraseArguments.builder()
                                .put(PERCENT, new PercentValue(0.1))
                                .build()
                )
        ));
    }

    @Test
    void schemaBudgetsLimitArgumentCountIdentifierLengthAndNestingDepth() {
        List<PhraseSlot<?>> tooMany = new ArrayList<>();
        for (int index = 0; index <= RulePhraseSchema.MAX_ARGUMENTS; index++) {
            tooMany.add(PhraseSlot.required("slot_" + index, NumberValue.class));
        }
        assertThrows(IllegalArgumentException.class, () -> new RulePhraseSchema(
                RulePhraseType.of("example", "too_many"),
                Set.of(PhraseVariant.DEFAULT),
                tooMany
        ));

        PhraseSlot<IdentifierValue> identifier = PhraseSlot.required(
                "identifier", IdentifierValue.class
        );
        RulePhraseSchema identifierSchema = new RulePhraseSchema(
                RulePhraseType.of("example", "identifier_budget"),
                Set.of(PhraseVariant.DEFAULT), List.of(identifier)
        );
        assertThrows(IllegalArgumentException.class, () -> identifierSchema.create(
                PhraseVariant.DEFAULT,
                PhraseArguments.builder().put(identifier, new IdentifierValue(
                        Identifier.fromNamespaceAndPath(
                                "example", "x".repeat(RulePhraseSchema.MAX_IDENTIFIER_LENGTH + 1)
                        )
                )).build()
        ));

        PhraseSlot<NestedPhraseValue> nested = PhraseSlot.required(
                "nested", NestedPhraseValue.class
        );
        RulePhraseType nestedType = RulePhraseType.of("example", "nested_budget");
        RulePhraseSchema nestedSchema = new RulePhraseSchema(
                nestedType, Set.of(PhraseVariant.DEFAULT), List.of(nested)
        );
        RulePhrase value = new RulePhrase(
                RulePhraseType.of("example", "leaf"),
                PhraseVariant.DEFAULT,
                PhraseArguments.EMPTY
        );
        for (int depth = 0; depth <= RulePhraseSchema.MAX_NESTING_DEPTH; depth++) {
            value = new RulePhrase(
                    nestedType,
                    PhraseVariant.DEFAULT,
                    PhraseArguments.builder()
                            .put(nested, new NestedPhraseValue(value))
                            .build()
            );
        }
        RulePhrase tooDeep = value;
        assertThrows(IllegalArgumentException.class, () -> nestedSchema.validate(
                PhraseVariant.DEFAULT, tooDeep.arguments()
        ));
    }

    @Test
    void everyBuiltInConditionAndOperationHasAProvider() throws Exception {
        RulePhraseRegistry registry = registry(true);
        assertEquals(identifierConstants(DamageNexusConditionIds.class),
                registry.conditionTypes());
        assertEquals(identifierConstants(DamageNexusOperationIds.class),
                registry.operationTypes());
    }

    @Test
    void signedValuesSelectSemanticVariants() {
        RulePhraseRegistry registry = registry(true);
        RulePhrase positive = registry.describeOperation(
                new AddChannelPostMultiplierOperation(id("fire"), 0.25f)
        ).orElseThrow();
        RulePhrase negative = registry.describeOperation(
                new AddChannelPostMultiplierOperation(id("fire"), -0.25f)
        ).orElseThrow();
        assertEquals(PhraseVariant.INCREASE, positive.variant());
        assertEquals(PhraseVariant.DECREASE, negative.variant());
        assertEquals(0.25, positive.arguments().get(PERCENT).orElseThrow().ratio(), 0.0001);
        assertEquals(0.25, negative.arguments().get(PERCENT).orElseThrow().ratio(), 0.0001);
    }

    @Test
    void customOperationCanReuseBuiltInPhraseType() {
        RulePhraseRegistry registry = registry(false);
        Identifier customId = Identifier.fromNamespaceAndPath("example", "frost_scaling");
        registry.registerOperation(customId, CustomOperation.class,
                (operation, phrases) -> phrases.create(
                        CHANGE_CHANNEL_DAMAGE,
                        operation.value() < 0 ? PhraseVariant.DECREASE : PhraseVariant.INCREASE,
                        PhraseArguments.builder()
                                .put(CHANNEL, new ChannelValue(operation.channel()))
                                .put(PERCENT, new PercentValue(Math.abs(operation.value())))
                                .build()
                ));
        registry.freeze();
        RulePhrase phrase = registry.describeOperation(
                new CustomOperation(customId, id("cold"), 0.4f)
        ).orElseThrow();
        assertEquals(CHANGE_CHANNEL_DAMAGE, phrase.type());
        assertEquals(PhraseVariant.INCREASE, phrase.variant());
    }

    @Test
    void customPhraseSchemaProviderAndLanguageTemplateRegisterOnce() throws Exception {
        RulePhraseRegistry registry = registry(false);
        RulePhraseType type = RulePhraseType.of("example", "echo_damage");
        PhraseSlot<NumberValue> echoes = PhraseSlot.required("echoes", NumberValue.class);
        registry.registerSchema(new RulePhraseSchema(
                type, Set.of(PhraseVariant.DEFAULT), List.of(echoes)
        ));
        Identifier operationId = Identifier.fromNamespaceAndPath("example", "echo");
        registry.registerOperation(operationId, CustomOperation.class,
                (operation, phrases) -> phrases.create(
                        type,
                        PhraseVariant.DEFAULT,
                        PhraseArguments.builder().put(echoes, new NumberValue(3)).build()
                ));
        registry.freeze();
        RulePhrase phrase = registry.describeOperation(
                new CustomOperation(operationId, id("fire"), 0)
        ).orElseThrow();
        String compactKey = type.translationKey(PhraseVariant.DEFAULT, PhraseForm.COMPACT);
        String detailKey = type.translationKey(PhraseVariant.DEFAULT, PhraseForm.DETAIL);
        try (TooltipTestLanguage ignored = TooltipTestLanguage.install("en_us")) {
            assertEquals("Rule phrase cannot be displayed",
                    new RulePhraseRenderer(registry, Locale.US)
                            .render(phrase, PhraseForm.COMPACT)
                            .orElseThrow().getString());
        }
        try (TooltipTestLanguage ignored = TooltipTestLanguage.installWith(
                "en_us", java.util.Map.of(
                        compactKey, "Echo %1$s times",
                        detailKey, "Repeat damage %1$s times"
                )
        )) {
            assertEquals("Echo 3 times", new RulePhraseRenderer(registry, Locale.US)
                    .render(phrase, PhraseForm.COMPACT).orElseThrow().getString());
            assertEquals("Repeat damage 3 times",
                    new RulePhraseRenderer(registry, Locale.US)
                            .render(phrase, PhraseForm.DETAIL)
                            .orElseThrow().getString());
        }
    }

    private static RulePhraseRegistry registry(boolean freeze) {
        RulePhraseRegistry registry = new RulePhraseRegistry();
        DamageNexusRulePhraseBootstrap.register(registry);
        if (freeze) {
            registry.freeze();
        }
        return registry;
    }

    private static PhraseArguments validArguments(RulePhraseSchema schema) {
        PhraseArguments.Builder builder = PhraseArguments.builder();
        for (PhraseSlot<?> slot : schema.slots()) {
            addSample(builder, slot);
        }
        return builder.build();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addSample(PhraseArguments.Builder builder, PhraseSlot slot) {
        builder.put(slot, sample(slot.valueType()));
    }

    private static PhraseValue sample(Class<?> type) {
        if (type == NumberValue.class) return new NumberValue(4);
        if (type == PercentValue.class) return new PercentValue(0.25);
        if (type == ChannelValue.class) return new ChannelValue(id("fire"));
        if (type == EntityRoleValue.class) return new EntityRoleValue(EntityRoleValue.Role.TARGET);
        if (type == EffectValue.class) return new EffectValue(id("effect"));
        if (type == EntityTypeValue.class) return new EntityTypeValue(id("entity"));
        if (type == TagValue.class) return new TagValue(TagValue.Kind.DAMAGE_TYPE, id("tag"));
        if (type == RequestKindValue.class) return new RequestKindValue(DamageRequestKind.PRIMARY);
        if (type == IdentifierValue.class) return new IdentifierValue(id("identifier"));
        if (type == MobCategoryValue.class) return new MobCategoryValue(MobCategory.MONSTER);
        throw new AssertionError("No sample for " + type);
    }

    private static Set<Identifier> identifierConstants(Class<?> owner) throws Exception {
        Set<Identifier> values = new java.util.HashSet<>();
        for (var field : owner.getFields()) {
            if (field.getType() == Identifier.class) {
                values.add((Identifier) field.get(null));
            }
        }
        return Set.copyOf(values);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("damagenexus_test", path);
    }

    private record CustomOperation(
            Identifier type,
            Identifier channel,
            float value
    ) implements DamageRuleOperation {
        @Override
        public DamageMutationResult apply(DamageRuleContext ctx) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        @Override
        public float stackingValue() {
            return value;
        }
    }
}
