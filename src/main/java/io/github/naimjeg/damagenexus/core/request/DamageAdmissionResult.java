package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import org.jspecify.annotations.Nullable;

/** Immutable result of one atomic managed-damage admission attempt. */
public record DamageAdmissionResult(
        boolean admitted,
        @Nullable DamageFailureReason reason,
        int rootDerivedCount,
        int serverTickCount
) {
    public DamageAdmissionResult {
        if (admitted == (reason != null)) {
            throw new IllegalArgumentException(
                    "Admitted results have no reason; rejected results require one"
            );
        }
        if (rootDerivedCount < 0 || serverTickCount < 0) {
            throw new IllegalArgumentException(
                    "Admission diagnostic counts must not be negative"
            );
        }
    }

    static DamageAdmissionResult admitted(int rootCount, int tickCount) {
        return new DamageAdmissionResult(true, null, rootCount, tickCount);
    }

    static DamageAdmissionResult rejected(
            DamageFailureReason reason,
            int rootCount,
            int tickCount
    ) {
        return new DamageAdmissionResult(
                false,
                reason,
                rootCount,
                tickCount
        );
    }
}
