package io.github.naimjeg.damagenexus.api.rule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.item.DamageNexusItemEntries;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AllOfCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AlwaysCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AttackerHealthBelowCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AnyOfCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.NotCondition;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRuleLimitsTest {

    @Test
    void conditionDepthSixteenIsAcceptedAndSeventeenRejected() {
        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(notChain(16)), operations(1))
        ).isEmpty());
        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(notChain(17)), operations(1))
        ).orElseThrow().contains("condition_depth=17"));
    }

    @Test
    void conditionNodeLimitAccepts256AndRejects257() {
        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(
                        List.of(conditionTree(15, 16)),
                        operations(1)
                )
        ).isEmpty());
        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(
                        List.of(conditionTree(16, 15)),
                        operations(1)
                )
        ).orElseThrow().contains("condition_nodes=257"));
    }

    @Test
    void compositeConditionsAreDefensivelyCopiedAndRejectNulls() {
        List<DamageRuleCondition> mutable = new ArrayList<>();
        DamageRuleCondition original = new AlwaysCondition();
        mutable.add(original);
        AllOfCondition allOf = new AllOfCondition(mutable);
        AnyOfCondition anyOf = new AnyOfCondition(mutable);
        mutable.set(0, new NotCondition(new AlwaysCondition()));
        mutable.clear();

        assertEquals(1, allOf.conditions().size());
        assertEquals(1, anyOf.conditions().size());
        assertEquals(original, allOf.conditions().getFirst());
        assertEquals(original, anyOf.conditions().getFirst());
        assertThrows(
                NullPointerException.class,
                () -> new AllOfCondition(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new AllOfCondition(
                        java.util.Collections.singletonList(null)
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new AnyOfCondition(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new AnyOfCondition(
                        java.util.Collections.singletonList(null)
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new NotCondition(null)
        );
    }

    @Test
    void thirdPartyCompositeParticipatesInDepthBudget() {
        DamageRuleCondition composite = composite(
                List.of(notChain(DamageRuleLimits.MAX_CONDITION_DEPTH))
        );
        String problem = DamageRuleLimits.findRuleProblem(
                rule(List.of(composite), operations(1))
        ).orElseThrow();
        assertTrue(problem.contains("condition_depth=17"));
    }

    @Test
    void invalidThirdPartyCompositeChildrenFailClosed() {
        CompositeDamageRuleCondition nullChildren = composite(null);
        CompositeDamageRuleCondition throwing =
                new CompositeDamageRuleCondition() {
                    @Override
                    public List<DamageRuleCondition> childConditions() {
                        throw new IllegalStateException("synthetic");
                    }

                    @Override
                    public Identifier type() {
                        return id("throwing_composite");
                    }

                    @Override
                    public boolean test(
                            io.github.naimjeg.damagenexus.api.context
                                    .DamageRuleContext ctx
                    ) {
                        return false;
                    }
                };

        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(nullChildren), operations(1))
        ).orElseThrow().contains("children_are_null"));
        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(throwing), operations(1))
        ).orElseThrow().contains("children_failed"));
    }

    @Test
    void thirdPartyCompositeCycleAndNullChildFailClosed() {
        java.util.concurrent.atomic.AtomicReference<DamageRuleCondition> self =
                new java.util.concurrent.atomic.AtomicReference<>();
        CompositeDamageRuleCondition cycle = new CompositeDamageRuleCondition() {
            @Override
            public List<DamageRuleCondition> childConditions() {
                return List.of(self.get());
            }

            @Override
            public Identifier type() {
                return id("cyclic_composite");
            }

            @Override
            public boolean test(
                    io.github.naimjeg.damagenexus.api.context.DamageRuleContext ctx
            ) {
                return true;
            }
        };
        self.set(cycle);
        CompositeDamageRuleCondition nullChild = composite(
                java.util.Collections.singletonList(null));

        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(cycle), operations(1))
        ).orElseThrow().contains("condition_cycle"));
        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(nullChild), operations(1))
        ).orElseThrow().contains("null_child"));
    }

    @Test
    void thirdPartyCompositeNodeBudgetAndTrustedLeafAreExplicit() {
        List<DamageRuleCondition> branches = java.util.stream.IntStream
                .range(0, 9)
                .mapToObj(branch -> (DamageRuleCondition) composite(
                        java.util.stream.IntStream.range(
                                        0,
                                        DamageRuleLimits.MAX_RULE_CONDITIONS)
                                .mapToObj(index ->
                                        (DamageRuleCondition) new AlwaysCondition())
                                .toList()))
                .toList();
        CompositeDamageRuleCondition overNodes = composite(branches);
        DamageRuleCondition trustedLeaf = new DamageRuleCondition() {
            @Override
            public Identifier type() {
                return id("trusted_leaf");
            }

            @Override
            public boolean test(
                    io.github.naimjeg.damagenexus.api.context.DamageRuleContext ctx
            ) {
                return true;
            }
        };

        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(overNodes), operations(1))
        ).orElseThrow().contains("condition_nodes=257"));
        DamageRuleLimits.RuleCost leafCost = DamageRuleLimits.measureRuleCost(
                rule(List.of(trustedLeaf), operations(1))
        ).orElseThrow();
        assertEquals(1, leafCost.conditionNodes());
    }

    @Test
    void thirdPartyCompositeFixtureReturnsImmutableChildren() {
        List<DamageRuleCondition> children = List.of(new AlwaysCondition());
        CompositeDamageRuleCondition fixture = composite(List.copyOf(children));

        assertThrows(UnsupportedOperationException.class,
                () -> fixture.childConditions().add(new AlwaysCondition()));
        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(fixture), operations(1))
        ).isEmpty());
    }

    @Test
    void sharedImmutableSubgraphRemainsValid() {
        DamageRuleCondition shared = new AllOfCondition(
                List.of(new AlwaysCondition())
        );
        AllOfCondition root = new AllOfCondition(
                List.of(shared, shared)
        );

        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(List.of(root), operations(1))
        ).isEmpty());
    }

    @Test
    void extremelyDeepRawInputFailsBeforeRecursiveCodec() {
        JsonObject root = new JsonObject();
        JsonObject cursor = root;

        for (int depth = 0; depth < 1_000; depth++) {
            JsonObject child = new JsonObject();
            cursor.add("nested", child);
            cursor = child;
        }

        assertTrue(assertDoesNotThrow(() ->
                DamageRuleDefinition.CODEC
                        .parse(JsonOps.INSTANCE, root)
                        .error()
                        .isPresent()
        ));
    }

    @Test
    void rawNodeBudgetIsChargedAtDiscoveryForWideLists() {
        JsonArray atLimit = new JsonArray();

        for (int index = 1;
                index < DamageRuleLimits.MAX_RAW_CODEC_NODES;
                index++) {
            atLimit.add(index);
        }

        assertTrue(DamageRuleLimits.rawProblemForTesting(
                JsonOps.INSTANCE,
                atLimit,
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                DamageRuleLimits.MAX_RAW_CODEC_NODES,
                DamageRuleLimits.RawTraversalObserver.NONE
        ).isEmpty());

        atLimit.add(0);
        AtomicInteger maxDiscovered = new AtomicInteger();
        AtomicInteger maxPending = new AtomicInteger();

        String problem = DamageRuleLimits.rawProblemForTesting(
                JsonOps.INSTANCE,
                atLimit,
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                DamageRuleLimits.MAX_RAW_CODEC_NODES,
                (discovered, pending) -> {
                    maxDiscovered.accumulateAndGet(
                            discovered,
                            Math::max
                    );
                    maxPending.accumulateAndGet(pending, Math::max);
                }
        ).orElseThrow();

        assertTrue(problem.contains(
                "raw_nodes="
                        + (DamageRuleLimits.MAX_RAW_CODEC_NODES + 1L)
        ));
        assertEquals(
                DamageRuleLimits.MAX_RAW_CODEC_NODES,
                maxDiscovered.get()
        );
        assertTrue(maxPending.get()
                <= DamageRuleLimits.MAX_RAW_CODEC_NODES);
    }

    @Test
    void rawMapWidthCountsKeysAndValues() {
        JsonObject atLimit = new JsonObject();

        int acceptedEntries =
                (DamageRuleLimits.MAX_RAW_CODEC_NODES - 1) / 2;

        for (int index = 0; index < acceptedEntries; index++) {
            atLimit.addProperty("key_" + index, index);
        }

        assertTrue(DamageRuleLimits.rawProblemForTesting(
                JsonOps.INSTANCE,
                atLimit,
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                DamageRuleLimits.MAX_RAW_CODEC_NODES,
                DamageRuleLimits.RawTraversalObserver.NONE
        ).isEmpty());

        atLimit.addProperty("over", 1);
        String problem = DamageRuleLimits.rawProblemForTesting(
                JsonOps.INSTANCE,
                atLimit,
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                DamageRuleLimits.MAX_RAW_CODEC_NODES,
                DamageRuleLimits.RawTraversalObserver.NONE
        ).orElseThrow();

        assertTrue(problem.contains(
                "raw_nodes="
                        + (DamageRuleLimits.MAX_RAW_CODEC_NODES + 1L)
                        + " maximum="
                        + DamageRuleLimits.MAX_RAW_CODEC_NODES
        ));
    }

    @Test
    void lazyUnboundedContainerStopsAtDiscoveryBudget() {
        JsonElement sentinel = new JsonPrimitive("lazy-unbounded-root");
        AtomicInteger enumerated = new AtomicInteger();
        AtomicInteger maximumPending = new AtomicInteger();
        DynamicOps<JsonElement> ops = countingUnboundedListOps(
                sentinel,
                enumerated
        );

        String problem = assertDoesNotThrow(() ->
                DamageRuleLimits.rawProblemForTesting(
                        ops,
                        sentinel,
                        DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                        DamageRuleLimits.MAX_RAW_CODEC_NODES,
                        (discovered, pending) ->
                                maximumPending.accumulateAndGet(
                                        pending,
                                        Math::max
                                )
                ).orElseThrow()
        );

        assertTrue(problem.contains(
                "raw_nodes="
                        + (DamageRuleLimits.MAX_RAW_CODEC_NODES + 1L)
        ));
        assertEquals(
                DamageRuleLimits.MAX_RAW_CODEC_NODES,
                enumerated.get(),
                "preflight enumerated beyond the first rejected child"
        );
        assertTrue(maximumPending.get()
                <= DamageRuleLimits.MAX_RAW_CODEC_NODES);
    }

    @Test
    void rawDepthBoundaryAndCombinedWidthDepthAttackAreBounded() {
        JsonElement atDepth = nestedArray(
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH
        );
        JsonElement overDepth = nestedArray(
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH + 1
        );

        assertTrue(DamageRuleLimits.rawProblemForTesting(
                JsonOps.INSTANCE,
                atDepth,
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                DamageRuleLimits.MAX_RAW_CODEC_NODES,
                DamageRuleLimits.RawTraversalObserver.NONE
        ).isEmpty());
        assertTrue(DamageRuleLimits.rawProblemForTesting(
                JsonOps.INSTANCE,
                overDepth,
                DamageRuleLimits.MAX_RAW_CODEC_DEPTH,
                DamageRuleLimits.MAX_RAW_CODEC_NODES,
                DamageRuleLimits.RawTraversalObserver.NONE
        ).orElseThrow().contains(
                "raw_depth="
                        + (DamageRuleLimits.MAX_RAW_CODEC_DEPTH + 1)
        ));

        JsonArray combined = new JsonArray();

        for (int index = 0; index < 20; index++) {
            combined.add(nestedArray(8));
        }

        AtomicInteger maximumPending = new AtomicInteger();
        assertTrue(DamageRuleLimits.rawProblemForTesting(
                JsonOps.INSTANCE,
                combined,
                16,
                32,
                (discovered, pending) ->
                        maximumPending.accumulateAndGet(
                                pending,
                                Math::max
                        )
        ).isPresent());
        assertTrue(maximumPending.get() <= 32);
    }

    @Test
    void ruleOperationListAcceptsBoundaryAndRejectsOneOver() {
        assertTrue(DamageRuleDefinition.CODEC
                .encodeStart(
                        JsonOps.INSTANCE,
                        rule(
                                List.of(),
                                operations(
                                        DamageRuleLimits.MAX_RULE_OPERATIONS
                                )
                        )
                )
                .result()
                .isPresent());
        assertTrue(DamageRuleDefinition.CODEC
                .encodeStart(
                        JsonOps.INSTANCE,
                        rule(
                                List.of(),
                                operations(
                                        DamageRuleLimits.MAX_RULE_OPERATIONS
                                                + 1
                                )
                        )
                )
                .error()
                .isPresent());
    }

    @Test
    void ruleConditionListAcceptsBoundaryAndRejectsOneOver() {
        assertTrue(DamageRuleDefinition.CODEC
                .encodeStart(
                        JsonOps.INSTANCE,
                        rule(
                                conditions(
                                        DamageRuleLimits.MAX_RULE_CONDITIONS
                                ),
                                operations(1)
                        )
                )
                .result()
                .isPresent());
        assertTrue(DamageRuleDefinition.CODEC
                .encodeStart(
                        JsonOps.INSTANCE,
                        rule(
                                conditions(
                                        DamageRuleLimits.MAX_RULE_CONDITIONS
                                                + 1
                                ),
                                operations(1)
                        )
                )
                .error()
                .isPresent());
    }

    @Test
    void itemAndNestedListsEnforceExactBoundaries() {
        DamageEntryDefinition entryAtLimit = entry(
                "entry_at_rule_limit",
                DamageRuleLimits.MAX_ENTRY_RULES
        );
        DamageEntryDefinition entryOverLimit = entry(
                "entry_over_rule_limit",
                DamageRuleLimits.MAX_ENTRY_RULES + 1
        );

        assertTrue(DamageEntryDefinition.STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, entryAtLimit)
                .result()
                .isPresent());
        assertTrue(DamageEntryDefinition.STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, entryOverLimit)
                .error()
                .isPresent());

        DamageAffixDefinition affixAtLimit = affix(
                "affix_at_entry_limit",
                DamageRuleLimits.MAX_AFFIX_ENTRIES
        );
        DamageAffixDefinition affixOverLimit = affix(
                "affix_over_entry_limit",
                DamageRuleLimits.MAX_AFFIX_ENTRIES + 1
        );

        assertTrue(DamageAffixDefinition.STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, affixAtLimit)
                .result()
                .isPresent());
        assertTrue(DamageAffixDefinition.STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, affixOverLimit)
                .error()
                .isPresent());

        DamageNexusItemEntries entriesAtLimit =
                new DamageNexusItemEntries(
                        List.copyOf(java.util.Collections.nCopies(
                                DamageRuleLimits.MAX_ITEM_ENTRIES,
                                entry("item_entry", 1)
                        )),
                        List.of()
                );
        DamageNexusItemEntries entriesOverLimit =
                new DamageNexusItemEntries(
                        List.copyOf(java.util.Collections.nCopies(
                                DamageRuleLimits.MAX_ITEM_ENTRIES + 1,
                                entry("item_entry_over", 1)
                        )),
                        List.of()
                );

        assertTrue(DamageNexusItemEntries.STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, entriesAtLimit)
                .result()
                .isPresent());
        assertTrue(DamageNexusItemEntries.STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, entriesOverLimit)
                .error()
                .isPresent());

        DamageNexusItemEntries affixesAtLimit =
                new DamageNexusItemEntries(
                        List.of(),
                        java.util.Collections.nCopies(
                                DamageRuleLimits.MAX_ITEM_AFFIXES,
                                affix("item_affix", 1)
                        )
                );
        DamageNexusItemEntries affixesOverLimit =
                new DamageNexusItemEntries(
                        List.of(),
                        java.util.Collections.nCopies(
                                DamageRuleLimits.MAX_ITEM_AFFIXES + 1,
                                affix("item_affix_over", 1)
                        )
                );

        assertTrue(DamageNexusItemEntries.STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, affixesAtLimit)
                .result()
                .isPresent());
        assertTrue(DamageNexusItemEntries.STORAGE_CODEC
                .encodeStart(JsonOps.INSTANCE, affixesOverLimit)
                .error()
                .isPresent());
    }

    @Test
    void displayAndNumericBudgetsRejectAbusiveValues() {
        JsonElement encodedLiteral = DisplayText.CODEC
                .encodeStart(JsonOps.INSTANCE, DisplayText.literal("text"))
                .result()
                .orElseThrow();
        assertTrue(encodedLiteral.isJsonObject());
        assertTrue(DisplayText.CODEC
                .parse(JsonOps.INSTANCE, new JsonPrimitive("text"))
                .error()
                .isPresent());

        DisplayText argsAtLimit = DisplayText.translatable(
                "test.translation",
                java.util.Collections.nCopies(
                        DamageRuleLimits.MAX_TRANSLATION_ARGS,
                        "arg"
                ).toArray(String[]::new)
        );
        DisplayText tooManyArgs = DisplayText.translatable(
                "test.translation",
                java.util.Collections.nCopies(
                        DamageRuleLimits.MAX_TRANSLATION_ARGS + 1,
                        "arg"
                ).toArray(String[]::new)
        );

        assertTrue(DisplayText.CODEC
                .encodeStart(JsonOps.INSTANCE, argsAtLimit)
                .result()
                .isPresent());
        assertTrue(DisplayText.CODEC
                .encodeStart(JsonOps.INSTANCE, tooManyArgs)
                .error()
                .isPresent());

        DamageEntryDisplay tooltipAtLimit = new DamageEntryDisplay(
                DisplayText.EMPTY,
                java.util.Collections.nCopies(
                        DamageRuleLimits.MAX_TOOLTIP_LINES,
                        DisplayText.literal("line")
                ),
                Optional.empty(),
                false
        );
        DamageEntryDisplay tooltipOverLimit = new DamageEntryDisplay(
                DisplayText.EMPTY,
                java.util.Collections.nCopies(
                        DamageRuleLimits.MAX_TOOLTIP_LINES + 1,
                        DisplayText.literal("line")
                ),
                Optional.empty(),
                false
        );

        assertTrue(DamageEntryDisplay.CODEC
                .encodeStart(JsonOps.INSTANCE, tooltipAtLimit)
                .result()
                .isPresent());
        assertTrue(DamageEntryDisplay.CODEC
                .encodeStart(JsonOps.INSTANCE, tooltipOverLimit)
                .error()
                .isPresent());

        String textAtLimit = "x".repeat(
                DamageRuleLimits.MAX_DISPLAY_CODE_POINTS
        );
        String textOverLimit = textAtLimit + "x";

        assertTrue(DisplayText.CODEC
                .encodeStart(
                        JsonOps.INSTANCE,
                        DisplayText.literal(textAtLimit)
                )
                .result()
                .isPresent());
        assertTrue(DisplayText.CODEC
                .encodeStart(
                        JsonOps.INSTANCE,
                        DisplayText.literal(textOverLimit)
                )
                .error()
                .isPresent());

        DamageRuleDefinition huge = new DamageRuleDefinition(
                id("huge_value"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(),
                List.of(DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID,
                        Float.MAX_VALUE
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );

        assertTrue(DamageRuleLimits.findRuleProblem(huge)
                .orElseThrow()
                .contains("operation_value_out_of_range"));

        assertTrue(DamageRuleLimits.findRuleProblem(
                rule(
                        List.of(new AttackerHealthBelowCondition(Float.NaN)),
                        operations(1)
                )
        ).orElseThrow().contains("condition_ratio_out_of_range"));
    }

    @Test
    void expandedRuleBudgetAccepts128AndRejects129Atomically() {
        List<DamageEntryDefinition> atLimit = List.of(
                entry("expanded_a", 32),
                entry("expanded_b", 32),
                entry("expanded_c", 32),
                entry("expanded_d", 32)
        );
        List<DamageEntryDefinition> overLimit = new ArrayList<>(atLimit);
        overLimit.add(entry("expanded_over", 1));

        assertTrue(DamageRuleLimits.findItemProblem(
                atLimit,
                List.of()
        ).isEmpty());
        assertTrue(DamageRuleLimits.findItemProblem(
                overLimit,
                List.of()
        ).orElseThrow().contains("expanded_rule_count=129"));
    }

    private static DamageRuleCondition notChain(int depth) {
        DamageRuleCondition condition = new AlwaysCondition();

        for (int index = 1; index < depth; index++) {
            condition = new NotCondition(condition);
        }

        return condition;
    }

    private static CompositeDamageRuleCondition composite(
            List<DamageRuleCondition> children
    ) {
        return new CompositeDamageRuleCondition() {
            @Override
            public List<DamageRuleCondition> childConditions() {
                return children;
            }

            @Override
            public Identifier type() {
                return id("third_party_composite");
            }

            @Override
            public boolean test(
                    io.github.naimjeg.damagenexus.api.context
                            .DamageRuleContext ctx
            ) {
                return false;
            }
        };
    }

    private static JsonElement nestedArray(int depth) {
        JsonElement value = new JsonPrimitive(true);

        for (int current = 1; current < depth; current++) {
            JsonArray parent = new JsonArray();
            parent.add(value);
            value = parent;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private static DynamicOps<JsonElement> countingUnboundedListOps(
            JsonElement sentinel,
            AtomicInteger enumerated
    ) {
        return (DynamicOps<JsonElement>) Proxy.newProxyInstance(
                DynamicOps.class.getClassLoader(),
                new Class<?>[]{DynamicOps.class},
                (proxy, method, arguments) -> {
                    if ("getStream".equals(method.getName())
                            && arguments != null
                            && arguments.length == 1
                            && arguments[0] == sentinel) {
                        return DataResult.success(Stream.generate(() -> {
                            enumerated.incrementAndGet();
                            return JsonNull.INSTANCE;
                        }));
                    }

                    try {
                        return method.invoke(JsonOps.INSTANCE, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }
        );
    }

    private static DamageRuleCondition conditionTree(
            int groupCount,
            int leavesPerGroup
    ) {
        List<DamageRuleCondition> groups = new ArrayList<>();

        for (int group = 0; group < groupCount; group++) {
            List<DamageRuleCondition> leaves = new ArrayList<>();

            for (int leaf = 0; leaf < leavesPerGroup; leaf++) {
                leaves.add(new AlwaysCondition());
            }

            groups.add(new AllOfCondition(List.copyOf(leaves)));
        }

        return new AllOfCondition(List.copyOf(groups));
    }

    private static List<DamageRuleOperation> operations(int count) {
        List<DamageRuleOperation> operations = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            operations.add(DamageNexusOperations.addBaseDamage(
                    DamageChannel.UNTYPED_ID,
                    1.0f
            ));
        }

        return List.copyOf(operations);
    }

    private static List<DamageRuleCondition> conditions(int count) {
        List<DamageRuleCondition> conditions = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            conditions.add(new AlwaysCondition());
        }

        return List.copyOf(conditions);
    }

    private static DamageRuleDefinition rule(
            List<DamageRuleCondition> conditions,
            List<DamageRuleOperation> operations
    ) {
        return new DamageRuleDefinition(
                id("limited_rule"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                conditions,
                operations,
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static DamageEntryDefinition entry(
            String path,
            int ruleCount
    ) {
        List<DamageRuleDefinition> rules = new ArrayList<>();

        for (int index = 0; index < ruleCount; index++) {
            rules.add(new DamageRuleDefinition(
                    id(path + "_rule_" + index),
                    DamageRuleRole.OFFENSIVE,
                    DamagePhase.BASE_MODIFICATION,
                    500,
                    List.of(),
                    operations(1),
                    DamageRuleStacking.STACK,
                    Optional.empty(),
                    Optional.empty()
            ));
        }

        return new DamageEntryDefinition(
                id(path),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                rules,
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageAffixDefinition affix(
            String path,
            int entryCount
    ) {
        List<DamageEntryDefinition> entries = new ArrayList<>();

        for (int index = 0; index < entryCount; index++) {
            entries.add(entry(path + "_entry_" + index, 1));
        }

        return new DamageAffixDefinition(
                id(path),
                DamageAffixDisplay.EMPTY,
                DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                entries,
                DamageAffixStacking.STACK,
                Optional.empty()
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
