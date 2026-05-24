package io.github.naimjeg.damagenexus.client.tooltip.narrative;

import io.github.naimjeg.damagenexus.api.client.phrase.RulePhrase;

import java.util.List;
import java.util.Objects;

public record RuleNarrative(
        ConditionExpression condition,
        List<RulePhrase> effects
) {
    public RuleNarrative {
        Objects.requireNonNull(condition, "condition");
        effects = effects == null ? List.of() : List.copyOf(effects);
    }
}
