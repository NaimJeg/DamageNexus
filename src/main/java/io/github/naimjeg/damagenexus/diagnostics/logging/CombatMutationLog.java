package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.trace.DamageMutationType;

public interface CombatMutationLog {

    void baseDamage(
            String sourceId,
            DamagePhase phase,
            DamageApplicationBucket applicationBucket,
            float value
    );

    void applicationPreMultiplier(
            String sourceId,
            DamagePhase phase,
            DamageApplicationBucket applicationBucket,
            int preMultiplierBucket,
            float value
    );

    void mutation(
            String sourceId,
            DamagePhase phase,
            DamageMutationType type,
            float value
    );

    void rejected(
            String action,
            DamagePhase currentPhase,
            String reason
    );
}

