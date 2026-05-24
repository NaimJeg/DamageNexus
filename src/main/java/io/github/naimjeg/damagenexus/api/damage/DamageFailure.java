package io.github.naimjeg.damagenexus.api.damage;

import java.util.Objects;

/** Immutable diagnostic accompanying a non-success result. */
public record DamageFailure(
        DamageFailureReason reason,
        String diagnostic
) {
    public DamageFailure {
        reason = Objects.requireNonNull(
                reason,
                "Damage failure reason must not be null"
        );
        diagnostic = Objects.requireNonNullElse(diagnostic, "");

        if (reason == DamageFailureReason.NONE) {
            throw new IllegalArgumentException(
                    "A damage failure cannot use the NONE reason"
            );
        }
    }
}
