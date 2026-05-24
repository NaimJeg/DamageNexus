package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionSnapshot;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable internal handoff from the pipeline to a public request submission.
 */
public record DamageExecutionSummary(
        float resolvedDamage,
        Map<Identifier, Float> resolvedChannelDamage,
        boolean critical,
        CriticalDecisionSnapshot criticalDecision,
        boolean cancelled,
        @Nullable String cancelSourceId
) {
    public DamageExecutionSummary {
        resolvedDamage = Float.isFinite(resolvedDamage)
                ? Math.max(0.0f, resolvedDamage)
                : 0.0f;
        resolvedChannelDamage = resolvedChannelDamage == null
                ? Map.of()
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(resolvedChannelDamage)
                );
        criticalDecision = criticalDecision == null
                ? CriticalDecisionSnapshot.unresolved()
                : criticalDecision;
    }

    public DamageExecutionSummary(
            float resolvedDamage,
            Map<Identifier, Float> resolvedChannelDamage,
            boolean critical,
            boolean cancelled,
            @Nullable String cancelSourceId
    ) {
        this(resolvedDamage, resolvedChannelDamage, critical,
                CriticalDecisionSnapshot.unresolved(), cancelled, cancelSourceId);
    }
}
