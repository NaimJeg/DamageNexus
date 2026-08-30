package io.github.naimjeg.damagenexus.client.tooltip.document;

import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.rule.CompositeDamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOrdering;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrativePlanner;
import io.github.naimjeg.damagenexus.config.TooltipDebugLevel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DamageTooltipDocumentPlanner {
    private final RuleNarrativePlanner narratives;

    public DamageTooltipDocumentPlanner(RuleNarrativePlanner narratives) {
        this.narratives = Objects.requireNonNull(narratives, "narratives");
    }

    public DamageTooltipDocument plan(
            List<DamageEntryDefinition> standaloneEntries,
            List<DamageAffixDefinition> affixes,
            List<VanillaTooltipAugmentation> vanillaAugmentations,
            DamageItemTemplateReferences references,
            TooltipDebugLevel debugLevel
    ) {
        List<EntryTooltipView> entryViews = safe(standaloneEntries).stream()
                .filter(Objects::nonNull)
                .sorted(DamageTooltipDocumentPlanner::compareEntryExecutionOrder)
                .map(this::entry)
                .toList();
        List<AffixTooltipView> affixViews = safe(affixes).stream()
                .filter(Objects::nonNull)
                .sorted(DamageTooltipDocumentPlanner::compareAffixExecutionOrder)
                .map(this::affix)
                .toList();
        DamageItemTemplateReferences safeReferences = references == null
                ? DamageItemTemplateReferences.EMPTY : references;
        List<TemplateReferenceTooltipView> templates = java.util.stream.Stream.concat(
                        safeReferences.entries().stream().map(reference ->
                                new TemplateReferenceTooltipView(
                                        TemplateReferenceTooltipView.Kind.ENTRY,
                                        reference.id()
                                )),
                        safeReferences.affixes().stream().map(reference ->
                                new TemplateReferenceTooltipView(
                                        TemplateReferenceTooltipView.Kind.AFFIX,
                                        reference.id()
                                ))
                )
                .toList();
        Optional<DebugTooltipSection> debug = debugLevel == null
                || debugLevel == TooltipDebugLevel.OFF
                ? Optional.empty()
                : Optional.of(new DebugTooltipSection());
        return new DamageTooltipDocument(
                affixViews,
                entryViews,
                safe(vanillaAugmentations),
                templates,
                debug
        );
    }

    private AffixTooltipView affix(DamageAffixDefinition affix) {
        return new AffixTooltipView(
                affix.id(),
                affix.display().name(),
                affix.display().authoredSummary(),
                affix.display().flavorText(),
                affix.slot(),
                affix.rarity(),
                affix.stacking(),
                affix.stackingGroup(),
                RuleBreakdownPolicy.fromDisplay(
                        affix.display().showRuleBreakdown(),
                        !affix.display().authoredSummary().isEmpty()
                ),
                affix.entries().stream()
                        .sorted(DamageTooltipDocumentPlanner::compareEntryExecutionOrder)
                        .map(this::entry)
                        .toList()
        );
    }

    private EntryTooltipView entry(DamageEntryDefinition entry) {
        return new EntryTooltipView(
                entry.id(),
                entry.display().name(),
                entry.display().authoredSummary(),
                entry.display().flavorText(),
                entry.slot(),
                entry.stacking(),
                entry.stackingGroup(),
                RuleBreakdownPolicy.fromDisplay(
                        entry.display().showRuleBreakdown(),
                        !entry.display().authoredSummary().isEmpty()
                ),
                sortedRules(entry).stream()
                        .map(this::rule)
                        .toList()
        );
    }

    private static int compareEntryExecutionOrder(
            DamageEntryDefinition first,
            DamageEntryDefinition second
    ) {
        int ruleComparison = DamageRuleOrdering.compareDefinitionSequences(
                sortedRules(first),
                sortedRules(second)
        );
        return ruleComparison != 0
                ? ruleComparison
                : first.id().compareNamespaced(second.id());
    }

    /**
     * Affix blocks deliberately remain contiguous. A multi-phase affix cannot
     * be both globally interleaved rule-by-rule and kept as one visual block,
     * so its contained execution-ordered rule sequence determines the block's
     * placement.
     */
    private static int compareAffixExecutionOrder(
            DamageAffixDefinition first,
            DamageAffixDefinition second
    ) {
        int ruleComparison = DamageRuleOrdering.compareDefinitionSequences(
                sortedRules(first),
                sortedRules(second)
        );
        return ruleComparison != 0
                ? ruleComparison
                : first.id().compareNamespaced(second.id());
    }

    private static List<DamageRuleDefinition> sortedRules(
            DamageEntryDefinition entry
    ) {
        return DamageRuleOrdering.sortedDefinitions(entry.rules());
    }

    private static List<DamageRuleDefinition> sortedRules(
            DamageAffixDefinition affix
    ) {
        return affix.entries().stream()
                .flatMap(entry -> entry.rules().stream())
                .sorted(DamageRuleOrdering.DEFINITION_EXECUTION_ORDER)
                .toList();
    }

    private RuleTooltipView rule(DamageRuleDefinition rule) {
        return new RuleTooltipView(
                rule.id(),
                narratives.plan(rule),
                rule.phase(),
                rule.role(),
                rule.priority(),
                rule.stacking(),
                rule.stackingGroup(),
                rule.traceLabel(),
                conditionTypes(rule.conditions()),
                rule.operations().stream().map(operation -> operation.type()).toList()
        );
    }

    private static List<net.minecraft.resources.Identifier> conditionTypes(
            List<DamageRuleCondition> conditions
    ) {
        java.util.ArrayList<net.minecraft.resources.Identifier> types =
                new java.util.ArrayList<>();
        for (DamageRuleCondition condition : safe(conditions)) {
            collectConditionTypes(condition, types);
        }
        return List.copyOf(types);
    }

    private static void collectConditionTypes(
            DamageRuleCondition condition,
            List<net.minecraft.resources.Identifier> types
    ) {
        if (condition == null) {
            return;
        }
        types.add(condition.type());
        if (condition instanceof CompositeDamageRuleCondition composite) {
            for (DamageRuleCondition child : composite.childConditions()) {
                collectConditionTypes(child, types);
            }
        }
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
