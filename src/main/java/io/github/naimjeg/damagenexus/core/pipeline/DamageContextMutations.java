package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.context.DamageMutationContext;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.pipeline.mutation.DamageMutationGuard;
import io.github.naimjeg.damagenexus.core.trace.DamageMutationType;
import io.github.naimjeg.damagenexus.diagnostics.logging.CombatTrace;

final class DamageContextMutations implements DamageMutationContext {

    private final DamageMutationApplier applier;
    private final DamagePhaseState phaseState;
    private final CombatTrace trace;
    private final DamageMutationGate gate;

    DamageContextMutations(
            DamagePacketState packet,
            DamageCombatState combat,
            DamagePipelineResult result,
            DamagePhaseState phaseState,
            CombatTrace trace
    ) {
        this.applier = new DamageMutationApplier(packet, combat, result);
        this.phaseState = phaseState;
        this.trace = trace;
        this.gate = new DamageMutationGate(phaseState, trace);
    }

    @Override
    public DamageMutationResult tryAddBaseDamage(
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId
    ) {
        DamageMutationResult phaseResult = gate.requirePhase(
                "addBaseDamage",
                DamagePhase.BASE_MODIFICATION
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        return addBaseDamageWithoutPhaseCheck(
                "addBaseDamage",
                channel,
                bucket,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddBaseDamage(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return tryAddBaseDamage(
                channel,
                DamageApplicationBucket.DN_RULE_BASE,
                value,
                sourceId
        );
    }

    DamageMutationResult addBaseDamageWithoutPhaseCheck(
            String action,
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId
    ) {
        DamageMutationResult offenseResult = gate.canModifyOffense(action);

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return gate.reject(action, channelResult, "null channel");
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        if (bucket == null) {
            return gate.reject(
                    action,
                    DamageMutationResult.REJECTED_NULL_APPLICATION_BUCKET,
                    "null application bucket"
            );
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite value");
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addBaseDamage(channel, bucket, value);

        trace.mutations().baseDamage(
                sourceId,
                phaseState.currentPhase(),
                bucket,
                value
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddChannelPreMultiplier(
            DamageChannel channel,
            int modifierId,
            float value,
            String sourceId
    ) {
        String action = "addChannelPreMultiplier";

        DamageMutationResult offenseResult = gate.canModifyOffense(action);

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = gate.requireMultiplierPhase(action);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return gate.reject(action, channelResult, "null channel");
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite value");
        }

        DamageMutationResult bucketResult =
                DamageMutationGuard.requirePreMultiplierBucket(modifierId);

        if (!bucketResult.applied()) {
            return gate.rejectInvalidPreBucket(action, modifierId);
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addChannelPreMultiplier(
                channel,
                modifierId,
                value
        );

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.CHANNEL_PRE_MULTIPLIER,
                value
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddApplicationPreMultiplier(
            DamageApplicationBucket bucket,
            int modifierId,
            float value,
            String sourceId
    ) {
        String action = "addApplicationPreMultiplier";

        DamageMutationResult offenseResult = gate.canModifyOffense(action);

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = gate.requireMultiplierPhase(action);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        if (bucket == null) {
            return gate.reject(
                    action,
                    DamageMutationResult.REJECTED_NULL_APPLICATION_BUCKET,
                    "null application bucket"
            );
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite value");
        }

        DamageMutationResult bucketResult =
                DamageMutationGuard.requirePreMultiplierBucket(modifierId);

        if (!bucketResult.applied()) {
            return gate.rejectInvalidPreBucket(action, modifierId);
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addApplicationPreMultiplier(
                bucket,
                modifierId,
                value
        );

        trace.mutations().applicationPreMultiplier(
                sourceId,
                phaseState.currentPhase(),
                bucket,
                modifierId,
                value
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddGlobalPreMultiplier(
            int modifierId,
            float value,
            String sourceId
    ) {
        String action = "addGlobalPreMultiplier";

        DamageMutationResult offenseResult = gate.canModifyOffense(action);

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = gate.requireMultiplierPhase(action);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite value");
        }

        DamageMutationResult bucketResult =
                DamageMutationGuard.requirePreMultiplierBucket(modifierId);

        if (!bucketResult.applied()) {
            return gate.rejectInvalidPreBucket(action, modifierId);
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addGlobalPreMultiplier(modifierId, value);

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.GLOBAL_PRE_MULTIPLIER,
                value
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddChannelPostMultiplier(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        String action = "addChannelPostMultiplier";

        DamageMutationResult offenseResult = gate.canModifyOffense(action);

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = gate.requireMultiplierPhase(action);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return gate.reject(action, channelResult, "null channel");
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite value");
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addChannelPostMultiplier(channel, value);

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.CHANNEL_POST_MULTIPLIER,
                value
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddGlobalPostMultiplier(
            float value,
            String sourceId
    ) {
        String action = "addGlobalPostMultiplier";

        DamageMutationResult offenseResult = gate.canModifyOffense(action);

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult =
                gate.requireGlobalPostMultiplierPhase(action);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite value");
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addGlobalPostMultiplier(value);

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.GLOBAL_POST_MULTIPLIER,
                value
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryConvertDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio,
            String sourceId
    ) {
        String action = "convertDamage";

        DamageMutationResult offenseResult = gate.canModifyOffense(action);

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = gate.requirePhase(
                action,
                DamagePhase.TYPE_SCALING
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        if (from == null || to == null) {
            return gate.reject(
                    action,
                    DamageMutationResult.REJECTED_NULL_CHANNEL,
                    "null source or target channel"
            );
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(ratio);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite ratio");
        }

        float clampedRatio = DamageMutationGuard.clampRatio(ratio);

        if (clampedRatio == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        float movedAmount = applier.convertDamage(
                from,
                to,
                clampedRatio
        );

        if (movedAmount <= 0.0f) {
            return DamageMutationResult.NO_OP_EMPTY_SOURCE;
        }

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.CONVERSION,
                movedAmount
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryGainExtraDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio,
            String sourceId
    ) {
        String action = "gainExtraDamage";

        DamageMutationResult offenseResult = gate.canModifyOffense(action);

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = gate.requirePhase(
                action,
                DamagePhase.TYPE_SCALING
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        if (from == null || to == null) {
            return gate.reject(
                    action,
                    DamageMutationResult.REJECTED_NULL_CHANNEL,
                    "null source or target channel"
            );
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(ratio);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite ratio");
        }

        float safeRatio = DamageMutationGuard.clampNonNegative(ratio);

        if (safeRatio == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        float extraAmount = applier.gainExtraDamage(
                from,
                to,
                safeRatio
        );

        if (extraAmount <= 0.0f) {
            return DamageMutationResult.NO_OP_EMPTY_SOURCE;
        }

        trace.mutations().baseDamage(
                sourceId,
                phaseState.currentPhase(),
                DamageApplicationBucket.DN_RULE_BASE,
                extraAmount
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddTrueDamage(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return tryAddBaseDamage(
                channel,
                DamageApplicationBucket.DN_TRUE_DAMAGE,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryMultiplyArmorEffectiveness(
            float multiplier,
            String sourceId
    ) {
        String action = "multiplyArmorEffectiveness";

        DamageMutationResult defenseResult = gate.canModifyDefense(action);

        if (!defenseResult.applied()) {
            return defenseResult;
        }

        DamageMutationResult phaseResult = gate.requirePhase(
                action,
                DamagePhase.MITIGATION_SETUP
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(multiplier);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite multiplier");
        }

        float safeMultiplier = DamageMutationGuard.clampNonNegative(multiplier);

        applier.multiplyArmorEffectiveness(safeMultiplier);

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.ARMOR_EFFECTIVENESS_MULTIPLIER,
                safeMultiplier
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddTemporaryResistance(
            DamageChannel channel,
            float rating,
            String sourceId
    ) {
        String action = "addTemporaryResistance";

        DamageMutationResult defenseResult = gate.canModifyDefense(action);

        if (!defenseResult.applied()) {
            return defenseResult;
        }

        DamageMutationResult phaseResult = gate.requirePhase(
                action,
                DamagePhase.MITIGATION_SETUP
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return gate.reject(action, channelResult, "null channel");
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(rating);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite value");
        }

        if (rating == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        if (!applier.addTemporaryResistance(channel, rating)) {
            return gate.rejectInactiveChannel(action, channel);
        }

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.TEMPORARY_RESISTANCE,
                rating
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddChannelMitigation(
            DamageChannel channel,
            float reductionPercent,
            String sourceId
    ) {
        String action = "addChannelMitigation";

        DamageMutationResult defenseResult = gate.canModifyDefense(action);

        if (!defenseResult.applied()) {
            return defenseResult;
        }

        DamageMutationResult phaseResult = gate.requirePhase(
                action,
                DamagePhase.MITIGATION_SETUP
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return gate.reject(action, channelResult, "null channel");
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(reductionPercent);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite reduction");
        }

        float safeReduction =
                DamageMutationGuard.clampReduction(reductionPercent);

        if (safeReduction == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        if (!applier.addChannelMitigation(channel, safeReduction)) {
            return gate.rejectInactiveChannel(action, channel);
        }

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.CHANNEL_MITIGATION,
                safeReduction
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddGlobalMitigation(
            float reductionPercent,
            String sourceId
    ) {
        String action = "addGlobalMitigation";

        DamageMutationResult defenseResult = gate.canModifyDefense(action);

        if (!defenseResult.applied()) {
            return defenseResult;
        }

        DamageMutationResult phaseResult = gate.requirePhase(
                action,
                DamagePhase.MITIGATION_SETUP
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(reductionPercent);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite reduction");
        }

        float safeReduction =
                DamageMutationGuard.clampReduction(reductionPercent);

        if (safeReduction == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addGlobalMitigation(safeReduction);

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.GLOBAL_MITIGATION,
                safeReduction
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryOverrideFinalDamage(
            float amount,
            String sourceId
    ) {
        String action = "overrideFinalDamage";

        DamageMutationResult phaseResult = gate.requirePhase(
                action,
                DamagePhase.FINAL_OVERRIDE
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(amount);

        if (!finiteResult.applied()) {
            return gate.reject(action, finiteResult, "non-finite amount");
        }

        float safeAmount = DamageMutationGuard.clampNonNegative(amount);
        applier.overrideFinalDamage(safeAmount);

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.FINAL_OVERRIDE,
                safeAmount
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryCancelDamage(String sourceId) {
        String action = "cancelDamage";

        DamageMutationResult phaseResult = gate.requirePhase(
                action,
                DamagePhase.FINAL_OVERRIDE
        );

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(action, sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        applier.cancelDamage(sourceId);

        trace.mutations().mutation(
                sourceId,
                phaseState.currentPhase(),
                DamageMutationType.CANCEL_DAMAGE,
                0.0f
        );

        return DamageMutationResult.APPLIED;
    }


    private DamageMutationResult requireSourceId(
            String action,
            String sourceId
    ) {
        if (sourceId != null && !sourceId.isBlank()) {
            return DamageMutationResult.APPLIED;
        }

        return gate.reject(
                action,
                DamageMutationResult.REJECTED_EMPTY_SOURCE_ID,
                "empty source id"
        );
    }


    DamageMutationResult requirePhase(
            String action,
            DamagePhase expected
    ) {
        return gate.requirePhase(action, expected);
    }

    void rejectState(
            String action,
            String reason
    ) {
        gate.rejected(action, reason);
    }

}
