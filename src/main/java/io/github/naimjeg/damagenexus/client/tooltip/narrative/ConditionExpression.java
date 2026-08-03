package io.github.naimjeg.damagenexus.client.tooltip.narrative;

import io.github.naimjeg.damagenexus.api.client.phrase.RulePhrase;

import java.util.List;
import java.util.Objects;

public sealed interface ConditionExpression permits
        ConditionExpression.Always,
        ConditionExpression.Phrase,
        ConditionExpression.AllOf,
        ConditionExpression.AnyOf,
        ConditionExpression.Not {

    record Always() implements ConditionExpression {
    }

    record Phrase(RulePhrase phrase) implements ConditionExpression {
        public Phrase {
            Objects.requireNonNull(phrase, "phrase");
        }
    }

    /** implicit is true for DamageRuleDefinition.conditions(). */
    record AllOf(
            List<ConditionExpression> children,
            boolean implicit
    ) implements ConditionExpression {
        public AllOf {
            children = children == null ? List.of() : List.copyOf(children);
        }
    }

    record AnyOf(List<ConditionExpression> children) implements ConditionExpression {
        public AnyOf {
            children = children == null ? List.of() : List.copyOf(children);
        }
    }

    record Not(ConditionExpression child) implements ConditionExpression {
        public Not {
            Objects.requireNonNull(child, "child");
        }
    }
}
