package io.github.naimjeg.damagenexus.core;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.trace.DamageMutationType;
import org.jspecify.annotations.Nullable;

public record DamageOperation(
        String source,
        DamagePhase phase,
        DamageMutationType type,
        float value,
        @Nullable DamageApplicationBucket applicationBucket,
        int preMultiplierBucket
) {
    public static final int NO_PRE_MULTIPLIER_BUCKET = -1;

    public DamageOperation(
            String source,
            DamagePhase phase,
            DamageMutationType type,
            float value
    ) {
        this(
                source,
                phase,
                type,
                value,
                null,
                NO_PRE_MULTIPLIER_BUCKET
        );
    }

    public static DamageOperation baseDamage(
            String source,
            DamagePhase phase,
            DamageApplicationBucket applicationBucket,
            float value
    ) {
        return new DamageOperation(
                source,
                phase,
                DamageMutationType.BASE_DAMAGE,
                value,
                applicationBucket,
                NO_PRE_MULTIPLIER_BUCKET
        );
    }

    public static DamageOperation applicationPreMultiplier(
            String source,
            DamagePhase phase,
            DamageApplicationBucket applicationBucket,
            int preMultiplierBucket,
            float value
    ) {
        return new DamageOperation(
                source,
                phase,
                DamageMutationType.APPLICATION_PRE_MULTIPLIER,
                value,
                applicationBucket,
                preMultiplierBucket
        );
    }
}
