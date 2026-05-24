package io.github.naimjeg.damagenexus.client.tooltip.narrative;

import io.github.naimjeg.damagenexus.api.client.phrase.PhraseArguments;
import io.github.naimjeg.damagenexus.api.client.phrase.PhraseVariant;
import io.github.naimjeg.damagenexus.api.client.phrase.RulePhrase;
import io.github.naimjeg.damagenexus.api.client.phrase.RulePhraseRegistry;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AllOfCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AlwaysCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AnyOfCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.NotCondition;
import io.github.naimjeg.damagenexus.client.tooltip.TooltipDetailLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.github.naimjeg.damagenexus.api.client.phrase.DamageNexusRulePhrases.UNKNOWN_CONDITION;
import static io.github.naimjeg.damagenexus.api.client.phrase.DamageNexusRulePhrases.UNKNOWN_EFFECT;

public final class RuleNarrativePlanner {
    private final RulePhraseRegistry registry;

    public RuleNarrativePlanner(RulePhraseRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public RuleNarrative plan(DamageRuleDefinition rule) {
        Objects.requireNonNull(rule, "rule");
        return plan(rule.conditions(), rule.operations());
    }

    public RuleNarrative plan(
            List<DamageRuleCondition> conditionValues,
            List<DamageRuleOperation> operationValues
    ) {
        List<DamageRuleCondition> safeConditions = conditionValues == null
                ? List.of() : List.copyOf(conditionValues);
        List<DamageRuleOperation> safeOperations = operationValues == null
                ? List.of() : List.copyOf(operationValues);
        List<ConditionExpression> conditions = safeConditions.stream()
                .map(this::condition)
                .toList();
        ConditionExpression root = conditions.isEmpty()
                ? new ConditionExpression.Always()
                : new ConditionExpression.AllOf(conditions, true);

        List<RulePhrase> effects = new ArrayList<>();
        for (DamageRuleOperation operation : safeOperations) {
            effects.add(registry.describeOperation(operation)
                    .orElseGet(() -> registry.create(
                            UNKNOWN_EFFECT,
                            PhraseVariant.DEFAULT,
                            PhraseArguments.EMPTY
                    )));
        }
        return new RuleNarrative(root, effects);
    }

    public NarrativeLayout layout(
            RuleNarrative narrative,
            TooltipDetailLevel detailLevel
    ) {
        Objects.requireNonNull(narrative, "narrative");
        Objects.requireNonNull(detailLevel, "detailLevel");
        if (detailLevel == TooltipDetailLevel.COMPACT
                && singleCondition(narrative.condition())
                && narrative.effects().size() == 1) {
            return NarrativeLayout.SINGLE_SENTENCE;
        }
        if (detailLevel == TooltipDetailLevel.EXPANDED
                && complex(narrative.condition())) {
            return NarrativeLayout.EXPANDED_TREE;
        }
        return NarrativeLayout.CONDITION_WITH_EFFECT_LIST;
    }

    private ConditionExpression condition(DamageRuleCondition condition) {
        if (condition instanceof AlwaysCondition) {
            return new ConditionExpression.Always();
        }
        if (condition instanceof AllOfCondition allOf) {
            return new ConditionExpression.AllOf(
                    allOf.conditions().stream().map(this::condition).toList(),
                    false
            );
        }
        if (condition instanceof AnyOfCondition anyOf) {
            return new ConditionExpression.AnyOf(
                    anyOf.conditions().stream().map(this::condition).toList()
            );
        }
        if (condition instanceof NotCondition not) {
            return new ConditionExpression.Not(condition(not.condition()));
        }
        RulePhrase phrase = registry.describeCondition(condition)
                .orElseGet(() -> registry.create(
                        UNKNOWN_CONDITION,
                        PhraseVariant.DEFAULT,
                        PhraseArguments.EMPTY
                ));
        return new ConditionExpression.Phrase(phrase);
    }

    private static boolean singleCondition(ConditionExpression expression) {
        return switch (expression) {
            case ConditionExpression.Always ignored -> false;
            case ConditionExpression.Phrase ignored -> true;
            case ConditionExpression.AllOf all -> all.children().size() == 1
                    && singleCondition(all.children().getFirst());
            default -> false;
        };
    }

    private static boolean complex(ConditionExpression expression) {
        return switch (expression) {
            case ConditionExpression.Always ignored -> false;
            case ConditionExpression.Phrase ignored -> false;
            case ConditionExpression.AllOf all -> all.children().size() > 1
                    || all.children().stream().anyMatch(RuleNarrativePlanner::complex);
            case ConditionExpression.AnyOf any -> true;
            case ConditionExpression.Not not -> complex(not.child())
                    || !(not.child() instanceof ConditionExpression.Phrase);
        };
    }
}
