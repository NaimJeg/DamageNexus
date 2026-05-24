package io.github.naimjeg.damagenexus.api.critical;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/** An immutable, registry-authored decision contribution. */
public record CriticalDecisionContribution(
        Identifier sourceId,
        int priority,
        CriticalDecision decision
) {
    public CriticalDecisionContribution {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(decision, "decision");
        if (decision == CriticalDecision.DEFAULT) {
            throw new IllegalArgumentException("DEFAULT is not a contribution");
        }
    }
}
