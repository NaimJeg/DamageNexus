package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.diagnostics.logging.CombatTrace;

/**
 * Centralizes mutation phase/lock rejection behavior for
 * {@link DamageContextMutations}.
 *
 * <p>This keeps mutation methods focused on applying state changes while this
 * class owns the common guard result + trace rejection formatting.</p>
 */
final class DamageMutationGate {

    private final DamagePhaseState phaseState;
    private final CombatTrace trace;

    DamageMutationGate(
            DamagePhaseState phaseState,
            CombatTrace trace
    ) {
        this.phaseState = phaseState;
        this.trace = trace;
    }

    DamageMutationResult canModifyOffense(String action) {
        DamageMutationResult result = phaseState.canModifyOffense();

        if (result.applied()) {
            return result;
        }

        return reject(
                action,
                result,
                "offensive damage already finalized"
        );
    }

    DamageMutationResult canModifyDefense(String action) {
        DamageMutationResult result = phaseState.canModifyDefense();

        if (result.applied()) {
            return result;
        }

        return reject(
                action,
                result,
                "defensive damage already calculated"
        );
    }

    DamageMutationResult requirePhase(
            String action,
            DamagePhase expected
    ) {
        DamageMutationResult result = phaseState.requirePhase(expected);

        if (result.applied()) {
            return result;
        }

        rejected(
                action,
                "expected phase " + expected
        );

        return result;
    }

    DamageMutationResult requireMultiplierPhase(String action) {
        DamageMutationResult result = phaseState.requireMultiplierPhase();

        if (result.applied()) {
            return result;
        }

        rejected(
                action,
                "expected phase TYPE_SCALING, CRITICAL_HIT, CONDITIONAL_MULTI, or GLOBAL_ADJUSTMENT"
        );

        return result;
    }

    DamageMutationResult requireGlobalPostMultiplierPhase(String action) {
        DamageMutationResult result =
                phaseState.requireGlobalPostMultiplierPhase();

        if (result.applied()) {
            return result;
        }

        rejected(
                action,
                "expected phase CONDITIONAL_MULTI or GLOBAL_ADJUSTMENT"
        );

        return result;
    }

    DamageMutationResult reject(
            String action,
            DamageMutationResult result,
            String reason
    ) {
        rejected(action, reason);
        return result;
    }

    DamageMutationResult rejectInvalidPreBucket(
            String action,
            int modifierId
    ) {
        rejected(
                action,
                "invalid pre-multiplier bucket id " + modifierId
        );

        return DamageMutationResult.REJECTED_INVALID_PRE_MULTIPLIER_BUCKET;
    }

    DamageMutationResult rejectInactiveChannel(
            String action,
            DamageChannel channel
    ) {
        rejected(
                action,
                "target channel is not active: "
                        + (channel == null ? "<null>" : channel.id())
        );

        return DamageMutationResult.REJECTED_INACTIVE_CHANNEL;
    }

    void rejected(
            String action,
            String reason
    ) {
        if (!trace.enabled()) {
            return;
        }

        trace.mutations().rejected(
                action,
                phaseState.currentPhase(),
                reason
        );
    }
}
