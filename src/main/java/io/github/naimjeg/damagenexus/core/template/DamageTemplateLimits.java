package io.github.naimjeg.damagenexus.core.template;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;

/** Shared aggregate limits for Java and datapack static templates. */
public final class DamageTemplateLimits {
    public static final int MAX_ENTRY_TEMPLATES = 256;
    public static final int MAX_AFFIX_TEMPLATES = 256;
    public static final int MAX_TEMPLATE_RULES = 8_192;
    public static final int MAX_TEMPLATE_CONDITION_NODES = 65_536;
    public static final int MAX_TEMPLATE_OPERATIONS = 32_768;

    private DamageTemplateLimits() {}

    /**
     * Validates one complete Java-plus-datapack registry candidate.
     * All accumulation uses checked {@code long} arithmetic.
     */
    public static Cost requireWithinLimits(
            Map<Identifier, DamageEntryDefinition> entries,
            Map<Identifier, DamageAffixDefinition> affixes
    ) {
        Map<Identifier, DamageEntryDefinition> safeEntries =
                Objects.requireNonNull(entries, "entries");
        Map<Identifier, DamageAffixDefinition> safeAffixes =
                Objects.requireNonNull(affixes, "affixes");
        requireMaximum("entry_templates", safeEntries.size(),
                MAX_ENTRY_TEMPLATES);
        requireMaximum("affix_templates", safeAffixes.size(),
                MAX_AFFIX_TEMPLATES);

        Accumulator accumulator = new Accumulator();
        for (DamageEntryDefinition definition : safeEntries.values()) {
            accumulator.addEntry(Objects.requireNonNull(
                    definition, "entry template"));
        }
        for (DamageAffixDefinition definition : safeAffixes.values()) {
            accumulator.addAffix(Objects.requireNonNull(
                    definition, "affix template"));
        }
        return new Cost(
                safeEntries.size(),
                safeAffixes.size(),
                accumulator.rules,
                accumulator.conditions,
                accumulator.operations
        );
    }

    private static void requireMaximum(
            String category,
            long actual,
            long maximum
    ) {
        if (actual > maximum) {
            throw exceeded(category, actual, maximum);
        }
    }

    private static IllegalArgumentException exceeded(
            String category,
            long actual,
            long maximum
    ) {
        return new IllegalArgumentException(
                "template_aggregate_budget_exceeded category=" + category
                        + " actual=" + actual
                        + " maximum=" + maximum
                        + " reason=aggregate_limit");
    }

    public record Cost(
            long entryTemplates,
            long affixTemplates,
            long rules,
            long conditionNodes,
            long operations
    ) {}

    private static final class Accumulator {
        private long rules;
        private long conditions;
        private long operations;

        private void addEntry(DamageEntryDefinition definition) {
            for (var rule : definition.rules()) {
                var measured = DamageRuleLimits.measureRuleCost(rule)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "template_rule_is_not_measurable id="
                                        + rule.id()));
                rules = checkedAdd(
                        "rules", rules, 1L, MAX_TEMPLATE_RULES);
                conditions = checkedAdd(
                        "condition_nodes", conditions,
                        measured.conditionNodes(),
                        MAX_TEMPLATE_CONDITION_NODES);
                operations = checkedAdd(
                        "operations", operations,
                        measured.operations(), MAX_TEMPLATE_OPERATIONS);
            }
        }

        private void addAffix(DamageAffixDefinition definition) {
            for (DamageEntryDefinition entry : definition.entries()) {
                addEntry(entry);
            }
        }

        private static long checkedAdd(
                String category,
                long current,
                long amount,
                long maximum
        ) {
            if (amount < 0L || current > Long.MAX_VALUE - amount) {
                throw exceeded(category, Long.MAX_VALUE, maximum);
            }
            long actual = current + amount;
            if (actual > maximum) {
                throw exceeded(category, actual, maximum);
            }
            return actual;
        }
    }
}
