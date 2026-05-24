package io.github.naimjeg.damagenexus.core.settlement;

import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.damagenexus.api.damage.DamageSettlementStatus;
import io.github.naimjeg.damagenexus.core.pipeline.DamageExecutionSummary;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Map;

/**
 * Framework-owned settlement state and the sole ordinary caller authorized to
 * complete a public settlement snapshot.
 */
@ApiStatus.Internal
public final class DamageSettlementState {

    private final DamageContainer container;
    private final LivingIncomingDamageEvent incomingEvent;
    private final DamageOrigin origin;
    private final LivingEntity target;
    private final ServerLevel level;
    private final float incomingHealth;
    private final float incomingAbsorption;

    private DamageSettlementPhase phase = DamageSettlementPhase.OPEN;
    private DamageExecutionSummary calculation;
    private float healthBefore;
    private float absorptionBefore;
    private boolean lateIncomingCancellation;
    private DamageSettlementCompletion completion;

    DamageSettlementState(
            LivingIncomingDamageEvent incomingEvent,
            DamageOrigin origin,
            LivingEntity target,
            ServerLevel level
    ) {
        this.incomingEvent = Objects.requireNonNull(
                incomingEvent,
                "incomingEvent"
        );
        this.container = incomingEvent.getContainer();
        this.origin = Objects.requireNonNull(origin, "origin");
        this.target = Objects.requireNonNull(target, "target");
        this.level = Objects.requireNonNull(level, "level");
        this.incomingHealth = safeAmount(target.getHealth());
        this.incomingAbsorption = safeAmount(target.getAbsorptionAmount());
        this.healthBefore = incomingHealth;
        this.absorptionBefore = incomingAbsorption;
    }

    DamageContainer container() {
        return container;
    }

    DamageSettlementPhase phase() {
        return phase;
    }

    void calculated(DamageExecutionSummary summary) {
        requirePhase(DamageSettlementPhase.OPEN, "calculate");
        this.calculation = Objects.requireNonNull(summary, "summary");
        this.phase = DamageSettlementPhase.CALCULATED;
    }

    void rejectAdmission(DamageFailureReason reason) {
        requirePhase(DamageSettlementPhase.OPEN, "reject admission for");
        Objects.requireNonNull(reason, "reason");
        phase = DamageSettlementPhase.ADMISSION_REJECTED;
        completion = new DamageSettlementCompletion(
                DamageSettlementSnapshot.completeInternal(
                        origin,
                        target,
                        level,
                        Map.of(),
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        incomingHealth,
                        incomingHealth,
                        incomingAbsorption,
                        incomingAbsorption,
                        false,
                        true,
                        false,
                        DamageSettlementStatus.NOT_APPLIED,
                        reason,
                        null
                ),
                this
        );
    }

    void markLateIncomingCancellation() {
        if (phase == DamageSettlementPhase.CALCULATED) {
            lateIncomingCancellation = true;
        }
    }

    void capturePre(LivingDamageEvent.Pre event) {
        requirePhase(DamageSettlementPhase.CALCULATED, "capture pre-apply");
        healthBefore = safeAmount(event.getEntity().getHealth());
        absorptionBefore = safeAmount(
                event.getEntity().getAbsorptionAmount()
        );
        phase = DamageSettlementPhase.PRE_APPLY_CAPTURED;
    }

    DamageSettlementCompletion capturePost(LivingDamageEvent.Post event) {
        requirePhase(
                DamageSettlementPhase.PRE_APPLY_CAPTURED,
                "capture post-apply"
        );

        float inflicted = safeAmount(event.getInflictedDamage());
        float healthAfter = safeAmount(target.getHealth());
        float absorptionAfter = safeAmount(target.getAbsorptionAmount());

        float actualHealthDamage = positiveDelta(
                healthBefore,
                healthAfter
        );
        float actualAbsorptionDamage = positiveDelta(
                absorptionBefore,
                absorptionAfter
        );

        DamageFailureReason reason = terminalReason(inflicted, false);
        DamageSettlementStatus status = reason == null
                ? DamageSettlementStatus.APPLIED
                : DamageSettlementStatus.NOT_APPLIED;
        phase = status == DamageSettlementStatus.APPLIED
                ? DamageSettlementPhase.APPLIED
                : DamageSettlementPhase.NOT_APPLIED;

        completion = new DamageSettlementCompletion(
                snapshot(
                        status,
                        reason,
                        inflicted,
                        actualHealthDamage,
                        actualAbsorptionDamage,
                        healthAfter,
                        absorptionAfter,
                        calculation.cancelled()
                ),
                this
        );
        return completion;
    }

    DamageSettlementCompletion completeWithoutPost(boolean vanillaAccepted) {
        if (phase == DamageSettlementPhase.ADMISSION_REJECTED) {
            return completion;
        }
        if (phase == DamageSettlementPhase.APPLIED
                || phase == DamageSettlementPhase.NOT_APPLIED) {
            return completion;
        }

        if (phase != DamageSettlementPhase.CALCULATED
                && phase != DamageSettlementPhase.PRE_APPLY_CAPTURED) {
            return null;
        }

        if (vanillaAccepted) {
            abort();
            return null;
        }

        if (incomingEvent.isCanceled()) {
            lateIncomingCancellation = true;
        }
        DamageFailureReason reason = terminalReason(0.0f, true);
        if (reason == null) {
            reason = DamageFailureReason.VANILLA_REJECTED;
        }

        phase = DamageSettlementPhase.NOT_APPLIED;
        completion = new DamageSettlementCompletion(
                snapshot(
                        DamageSettlementStatus.NOT_APPLIED,
                        reason,
                        0.0f,
                        0.0f,
                        0.0f,
                        safeAmount(target.getHealth()),
                        safeAmount(target.getAbsorptionAmount()),
                        calculation.cancelled()
                                || lateIncomingCancellation
                ),
                this
        );
        return completion;
    }

    void abort() {
        phase = DamageSettlementPhase.ABORTED;
        completion = null;
    }

    void markPublished() {
        if (phase != DamageSettlementPhase.ADMISSION_REJECTED
                && phase != DamageSettlementPhase.APPLIED
                && phase != DamageSettlementPhase.NOT_APPLIED) {
            throw new IllegalStateException(
                    "Cannot publish settlement in phase " + phase
            );
        }
        phase = DamageSettlementPhase.PUBLISHED;
    }

    private DamageFailureReason terminalReason(
            float inflicted,
            boolean noPost
    ) {
        if (calculation.cancelled()) {
            return DamageFailureReason.CANCELLED;
        }
        if (calculation.resolvedDamage() <= 0.0f) {
            return DamageFailureReason.ZERO_DAMAGE;
        }
        if (lateIncomingCancellation) {
            return DamageFailureReason.LATE_INCOMING_CANCELLATION;
        }
        if (!noPost && inflicted <= 0.0f) {
            return DamageFailureReason.ZERO_AFTER_PRE;
        }
        if (noPost) {
            return DamageFailureReason.VANILLA_REJECTED;
        }
        return null;
    }

    private DamageSettlementSnapshot snapshot(
            DamageSettlementStatus status,
            DamageFailureReason reason,
            float appliedDamage,
            float healthDamage,
            float absorptionDamage,
            float healthAfter,
            float absorptionAfter,
            boolean cancelled
    ) {
        return DamageSettlementSnapshot.completeInternal(
                origin,
                target,
                level,
                calculation.resolvedChannelDamage(),
                calculation.resolvedDamage(),
                appliedDamage,
                healthDamage,
                absorptionDamage,
                healthBefore,
                healthAfter,
                absorptionBefore,
                absorptionAfter,
                calculation.critical(),
                calculation.criticalDecision(),
                cancelled,
                true,
                status,
                reason,
                calculation.cancelSourceId()
        );
    }

    private void requirePhase(
            DamageSettlementPhase expected,
            String action
    ) {
        if (phase != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " settlement in phase " + phase
            );
        }
    }

    private static float safeAmount(float amount) {
        return Float.isFinite(amount) ? Math.max(0.0f, amount) : 0.0f;
    }

    private static float positiveDelta(float before, float after) {
        return safeAmount(before - after);
    }
}
