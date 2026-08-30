package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Shared ordering rules for presenting DamageNexus rule execution.
 *
 * <p>The declaration order of {@link DamagePhase} is the pipeline order.
 * Rule identifiers are used only as a deterministic presentation tie-break;
 * they are not an additional runtime priority.</p>
 */
public final class DamageRuleOrdering {

    public static final Comparator<DamageRuleDefinition>
            DEFINITION_EXECUTION_ORDER =
            DamageRuleOrdering::compareDefinitionExecutionOrder;

    private DamageRuleOrdering() {
    }

    /**
     * Compares rules by pipeline phase, descending priority, then identifier.
     */
    public static int compareDefinitionExecutionOrder(
            DamageRuleDefinition first,
            DamageRuleDefinition second
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        int phaseComparison = Integer.compare(
                phaseOrder(first.phase()),
                phaseOrder(second.phase())
        );
        if (phaseComparison != 0) {
            return phaseComparison;
        }

        int priorityComparison = compareSamePhasePriorityDescending(
                first,
                second
        );
        if (priorityComparison != 0) {
            return priorityComparison;
        }

        return first.id().compareNamespaced(second.id());
    }

    /**
     * Runtime processors already operate one phase at a time, so they share
     * this priority contract without imposing the presentation-only ID tie.
     */
    public static int compareSamePhasePriorityDescending(
            DamageRuleDefinition first,
            DamageRuleDefinition second
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return Integer.compare(second.priority(), first.priority());
    }

    public static int phaseOrder(DamagePhase phase) {
        return Objects.requireNonNull(phase, "phase").ordinal();
    }

    /**
     * Returns an immutable execution-ordered view without changing authored
     * rule list order.
     */
    public static List<DamageRuleDefinition> sortedDefinitions(
            List<DamageRuleDefinition> definitions
    ) {
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }

        return definitions.stream()
                .filter(Objects::nonNull)
                .sorted(DEFINITION_EXECUTION_ORDER)
                .toList();
    }

    /**
     * Lexicographically compares already execution-ordered rule sequences.
     * An empty sequence sorts after any non-empty sequence so malformed or
     * future empty presentation groups do not displace executable groups.
     */
    public static int compareDefinitionSequences(
            List<DamageRuleDefinition> first,
            List<DamageRuleDefinition> second
    ) {
        List<DamageRuleDefinition> safeFirst = first == null ? List.of() : first;
        List<DamageRuleDefinition> safeSecond = second == null ? List.of() : second;

        if (safeFirst.isEmpty() || safeSecond.isEmpty()) {
            if (safeFirst.isEmpty() && safeSecond.isEmpty()) {
                return 0;
            }
            return safeFirst.isEmpty() ? 1 : -1;
        }

        int sharedSize = Math.min(safeFirst.size(), safeSecond.size());
        for (int index = 0; index < sharedSize; index++) {
            int comparison = compareDefinitionExecutionOrder(
                    safeFirst.get(index),
                    safeSecond.get(index)
            );
            if (comparison != 0) {
                return comparison;
            }
        }

        return Integer.compare(safeFirst.size(), safeSecond.size());
    }
}
