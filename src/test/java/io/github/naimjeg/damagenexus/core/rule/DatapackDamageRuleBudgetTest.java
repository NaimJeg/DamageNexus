package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AlwaysCondition;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatapackDamageRuleBudgetTest {

    @BeforeEach
    @AfterEach
    void resetStore() {
        DatapackDamageRuleStore.replace(List.of());
        DamageNexusDiagnosticState.clearAll();
    }

    @Test
    void exactRuleBoundaryPublishesAndOneOverRetainsOldSnapshot() {
        Map<Identifier, DamageRuleDefinition> atLimit =
                rules(
                        DatapackDamageRuleReloadListener
                                .MAX_DATAPACK_RULES,
                        0,
                        1
                );

        assertTrue(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(atLimit));
        assertEquals(
                DatapackDamageRuleReloadListener.MAX_DATAPACK_RULES,
                DatapackDamageRuleStore.ruleCount()
        );

        List<DamageRuleDefinition> previous =
                DatapackDamageRuleStore.rules();
        Map<Identifier, DamageRuleDefinition> over =
                rules(
                        DatapackDamageRuleReloadListener
                                .MAX_DATAPACK_RULES + 1,
                        0,
                        1
                );

        assertFalse(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(over));
        assertEquals(previous, DatapackDamageRuleStore.rules());
        assertEquals(
                previous,
                DatapackDamageRuleStore.rules(
                        DamagePhase.BASE_MODIFICATION
                )
        );
    }

    @Test
    void conditionAndOperationAggregateBoundariesAreExact() {
        int conditionRules =
                DatapackDamageRuleReloadListener
                        .MAX_DATAPACK_CONDITION_NODES
                        / io.github.naimjeg.damagenexus.api.rule
                        .DamageRuleLimits.MAX_RULE_CONDITIONS;
        Map<Identifier, DamageRuleDefinition> conditionsAtLimit =
                rules(
                        conditionRules,
                        io.github.naimjeg.damagenexus.api.rule
                                .DamageRuleLimits.MAX_RULE_CONDITIONS,
                        1
                );
        assertTrue(DatapackDamageRuleReloadListener
                .findAggregateProblem(conditionsAtLimit)
                .isEmpty());
        assertTrue(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(conditionsAtLimit));
        List<DamageRuleDefinition> conditionSnapshot =
                DatapackDamageRuleStore.rules();

        Map<Identifier, DamageRuleDefinition> conditionsOver =
                new LinkedHashMap<>(conditionsAtLimit);
        conditionsOver.put(
                id("condition_over_file"),
                rule("condition_over", 1, 1)
        );
        assertTrue(DatapackDamageRuleReloadListener
                .findAggregateProblem(conditionsOver)
                .orElseThrow()
                .contains("datapack_condition_nodes=8193"));
        assertFalse(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(conditionsOver));
        assertEquals(
                conditionSnapshot,
                DatapackDamageRuleStore.rules()
        );

        int operationRules =
                DatapackDamageRuleReloadListener.MAX_DATAPACK_OPERATIONS
                        / io.github.naimjeg.damagenexus.api.rule
                        .DamageRuleLimits.MAX_RULE_OPERATIONS;
        Map<Identifier, DamageRuleDefinition> operationsAtLimit =
                rules(
                        operationRules,
                        0,
                        io.github.naimjeg.damagenexus.api.rule
                                .DamageRuleLimits.MAX_RULE_OPERATIONS
                );
        assertTrue(DatapackDamageRuleReloadListener
                .findAggregateProblem(operationsAtLimit)
                .isEmpty());
        assertTrue(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(operationsAtLimit));
        List<DamageRuleDefinition> operationSnapshot =
                DatapackDamageRuleStore.rules();

        Map<Identifier, DamageRuleDefinition> operationsOver =
                new LinkedHashMap<>(operationsAtLimit);
        operationsOver.put(
                id("operation_over_file"),
                rule("operation_over", 0, 1)
        );
        assertTrue(DatapackDamageRuleReloadListener
                .findAggregateProblem(operationsOver)
                .orElseThrow()
                .contains("datapack_operations=4097"));
        assertFalse(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(operationsOver));
        assertEquals(
                operationSnapshot,
                DatapackDamageRuleStore.rules()
        );
    }

    @Test
    void duplicateAndInvalidRulesCannotBypassInputBudget() {
        Map<Identifier, DamageRuleDefinition> duplicates =
                new LinkedHashMap<>();

        for (int index = 0;
                index <= DatapackDamageRuleReloadListener
                .MAX_DATAPACK_RULES;
                index++) {
            duplicates.put(
                    id("duplicate_file_" + index),
                    ruleWithId("same_rule", 0, 1)
            );
        }

        assertTrue(DatapackDamageRuleReloadListener
                .findAggregateProblem(duplicates)
                .orElseThrow()
                .contains("datapack_rule_count=513"));

        Map<Identifier, DamageRuleDefinition> invalid =
                new LinkedHashMap<>();

        for (int index = 0;
                index <= DatapackDamageRuleReloadListener
                .MAX_DATAPACK_RULES;
                index++) {
            invalid.put(
                    id("invalid_file_" + index),
                    invalidCancelRule("invalid_" + index)
            );
        }

        assertTrue(DatapackDamageRuleReloadListener
                .findAggregateProblem(invalid)
                .orElseThrow()
                .contains("datapack_rule_count=513"));
    }

    @Test
    void aggregateRejectionDiagnosticIsBoundedAndPhaseViewIsCompact()
            throws Exception {
        DatapackDamageRuleStore.replace(List.of(
                rule("base", 0, 1),
                finalRule("final")
        ));
        assertEquals(
                1,
                DatapackDamageRuleStore
                        .rules(DamagePhase.BASE_MODIFICATION)
                        .size()
        );
        assertEquals(
                1,
                DatapackDamageRuleStore
                        .rules(DamagePhase.FINAL_OVERRIDE)
                        .size()
        );

        Map<Identifier, DamageRuleDefinition> over =
                rules(
                        DatapackDamageRuleReloadListener
                                .MAX_DATAPACK_RULES + 1,
                        0,
                        1
                );
        assertFalse(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(over));
        assertFalse(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(over));

        Method size = DamageNexusDiagnosticState.class
                .getDeclaredMethod(
                        "domainSize",
                        DamageNexusDiagnosticState.Domain.class
                );
        size.setAccessible(true);
        assertEquals(
                1,
                size.invoke(
                        null,
                        DamageNexusDiagnosticState.Domain
                                .DATAPACK_RELOAD
                )
        );
        assertTrue(DatapackDamageRuleReloadListener
                .findAggregateProblem(over)
                .orElseThrow()
                .length() <= 256);
    }

    @Test
    void legalReloadReplacesAllPhaseIndexesAtOnce() {
        DatapackDamageRuleStore.replace(List.of(rule("old", 0, 1)));
        Map<Identifier, DamageRuleDefinition> replacement =
                new LinkedHashMap<>();
        DamageRuleDefinition nextBase = rule("next_base", 0, 1);
        DamageRuleDefinition nextFinal = finalRule("next_final");
        replacement.put(id("next_base_file"), nextBase);
        replacement.put(id("next_final_file"), nextFinal);

        assertTrue(DatapackDamageRuleReloadListener
                .applyPreparedForTesting(replacement));
        assertEquals(List.of(nextBase, nextFinal),
                DatapackDamageRuleStore.rules());
        assertEquals(List.of(nextBase), DatapackDamageRuleStore.rules(
                DamagePhase.BASE_MODIFICATION
        ));
        assertEquals(List.of(nextFinal), DatapackDamageRuleStore.rules(
                DamagePhase.FINAL_OVERRIDE
        ));
    }

    @Test
    void concurrentReadersObserveOnlyCompleteSnapshots()
            throws Exception {
        List<DamageRuleDefinition> first = new ArrayList<>();
        List<DamageRuleDefinition> second = new ArrayList<>();

        for (int index = 0; index < 7; index++) {
            first.add(rule("first_" + index, 0, 1));
        }

        for (int index = 0; index < 11; index++) {
            second.add(finalRule("second_" + index));
        }

        DatapackDamageRuleStore.replace(first);
        int readers = 4;
        int iterations = 2_000;
        ExecutorService executor = Executors.newFixedThreadPool(readers + 1);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Throwable> failures =
                new ConcurrentLinkedQueue<>();

        executor.submit(() -> {
            await(start, failures);

            for (int index = 0; index < iterations; index++) {
                DatapackDamageRuleStore.replace(
                        (index & 1) == 0 ? first : second
                );
            }
        });

        for (int reader = 0; reader < readers; reader++) {
            executor.submit(() -> {
                await(start, failures);

                for (int index = 0; index < iterations; index++) {
                    List<DamageRuleDefinition> observed =
                            DatapackDamageRuleStore.rules();

                    if (!observed.equals(first)
                            && !observed.equals(second)) {
                        failures.add(new AssertionError(
                                "Observed partially published rule snapshot"
                        ));
                        return;
                    }

                    List<DamageRuleDefinition> base =
                            DatapackDamageRuleStore.rules(
                                    DamagePhase.BASE_MODIFICATION
                            );
                    List<DamageRuleDefinition> fin =
                            DatapackDamageRuleStore.rules(
                                    DamagePhase.FINAL_OVERRIDE
                            );

                    if ((!base.isEmpty() && !base.equals(first))
                            || (!fin.isEmpty() && !fin.equals(second))) {
                        failures.add(new AssertionError(
                                "Observed partially built phase index"
                        ));
                        return;
                    }
                }
            });
        }

        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        assertTrue(failures.isEmpty(), () -> String.valueOf(failures.peek()));
    }

    private static void await(
            CountDownLatch start,
            ConcurrentLinkedQueue<Throwable> failures
    ) {
        try {
            start.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            failures.add(exception);
        }
    }

    private static Map<Identifier, DamageRuleDefinition> rules(
            int count,
            int conditions,
            int operations
    ) {
        Map<Identifier, DamageRuleDefinition> rules =
                new LinkedHashMap<>();

        for (int index = 0; index < count; index++) {
            rules.put(
                    id("file_" + index),
                    rule("rule_" + index, conditions, operations)
            );
        }

        return rules;
    }

    private static DamageRuleDefinition rule(
            String path,
            int conditionCount,
            int operationCount
    ) {
        return ruleWithId(path, conditionCount, operationCount);
    }

    private static DamageRuleDefinition ruleWithId(
            String path,
            int conditionCount,
            int operationCount
    ) {
        List<DamageRuleCondition> conditions =
                java.util.stream.IntStream
                        .range(0, conditionCount)
                        .mapToObj(index -> new AlwaysCondition())
                        .map(DamageRuleCondition.class::cast)
                        .toList();
        List<DamageRuleOperation> operations =
                java.util.stream.IntStream
                        .range(0, operationCount)
                        .mapToObj(index ->
                                DamageNexusOperations.addBaseDamage(
                                        DamageChannel.UNTYPED_ID,
                                        1.0f
                                ))
                        .map(DamageRuleOperation.class::cast)
                        .toList();

        return new DamageRuleDefinition(
                id(path),
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

    private static DamageRuleDefinition invalidCancelRule(
            String path
    ) {
        return new DamageRuleDefinition(
                id(path),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(),
                List.of(DamageNexusOperations.cancelDamage()),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static DamageRuleDefinition finalRule(String path) {
        return new DamageRuleDefinition(
                id(path),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.FINAL_OVERRIDE,
                500,
                List.of(),
                List.of(DamageNexusOperations.cancelDamage()),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
