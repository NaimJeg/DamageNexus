package io.github.naimjeg.damagenexus.core.pipeline.mutation;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;

public final class DamageMutationGuard {

    private DamageMutationGuard() {
    }

    public static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    public static DamageMutationResult requirePhase(
            DamagePhase current,
            DamagePhase required
    ) {
        return current == required
                ? DamageMutationResult.APPLIED
                : DamageMutationResult.REJECTED_WRONG_PHASE;
    }

    public static DamageMutationResult requireChannel(
            DamageChannel channel
    ) {
        return channel != null
                ? DamageMutationResult.APPLIED
                : DamageMutationResult.REJECTED_NULL_CHANNEL;
    }

    public static DamageMutationResult requireFinite(float value) {
        return isFinite(value)
                ? DamageMutationResult.APPLIED
                : DamageMutationResult.REJECTED_NON_FINITE;
    }

    public static DamageMutationResult requirePreMultiplierBucket(
            int modifierId
    ) {
        PreMultiplierBucketRegistry.requireFrozen();

        return modifierId >= 0 && modifierId < PreMultiplierBucketRegistry.bucketCount()
                ? DamageMutationResult.APPLIED
                : DamageMutationResult.REJECTED_INVALID_PRE_MULTIPLIER_BUCKET;
    }

    public static float clampRatio(float ratio) {
        return Math.max(0.0f, Math.min(1.0f, ratio));
    }

    public static float clampReduction(float reduction) {
        return Math.max(-1.0f, Math.min(1.0f, reduction));
    }

    public static float clampNonNegative(float amount) {
        return Math.max(0.0f, amount);
    }
}
