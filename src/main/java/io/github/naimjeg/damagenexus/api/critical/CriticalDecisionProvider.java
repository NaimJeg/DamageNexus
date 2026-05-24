package io.github.naimjeg.damagenexus.api.critical;

import io.github.naimjeg.damagenexus.api.context.DamageContextView;

/**
 * Server-side callback that contributes only to the critical decision.
 * A callback may repeat one value idempotently. If it contributes FORCE and
 * SUPPRESS in the same invocation, SUPPRESS is committed. If it throws, none
 * of that callback's local contributions are committed in tolerant mode.
 */
@FunctionalInterface
public interface CriticalDecisionProvider {
    void contribute(DamageContextView context, CriticalDecisionCollector collector);
}
