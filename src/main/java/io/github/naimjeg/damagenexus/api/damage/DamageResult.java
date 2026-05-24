package io.github.naimjeg.damagenexus.api.damage;

import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionSnapshot;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * Structured result returned by {@code DamageNexusApi.submitDamage}.
 *
 * <p>Managed submissions expose their immutable completed settlement through
 * {@link #settlement()}. Requests rejected before entering the pipeline, and
 * calls for which no managed pipeline was observed, have no settlement.</p>
 */
public final class DamageResult {

    private final DamageRequest request;
    private final DamageSubmissionStatus status;
    private final Optional<DamageFailure> failure;
    private final boolean pipelineExecuted;
    private final boolean vanillaAccepted;
    private final float resolvedDamage;
    private final boolean critical;
    private final boolean cancelled;
    private final Optional<DamageSettlementSnapshot> settlement;

    private DamageResult(
            DamageRequest request,
            DamageSubmissionStatus status,
            DamageFailure failure,
            boolean pipelineExecuted,
            boolean vanillaAccepted,
            float resolvedDamage,
            boolean critical,
            boolean cancelled,
            DamageSettlementSnapshot settlement
    ) {
        this.request = Objects.requireNonNull(
                request,
                "Damage result request must not be null"
        );
        this.status = Objects.requireNonNull(
                status,
                "Damage result status must not be null"
        );
        this.failure = Optional.ofNullable(failure);
        this.pipelineExecuted = pipelineExecuted;
        this.vanillaAccepted = vanillaAccepted;
        this.resolvedDamage = sanitize(resolvedDamage);
        this.critical = critical;
        this.cancelled = cancelled;
        this.settlement = Optional.ofNullable(settlement);

        boolean successful = status == DamageSubmissionStatus.APPLIED;
        if (successful == this.failure.isPresent()) {
            throw new IllegalArgumentException(
                    "Applied results cannot have a failure and all other "
                            + "results require one"
            );
        }

        boolean managedSettlement = status == DamageSubmissionStatus.APPLIED
                || status == DamageSubmissionStatus.NOT_APPLIED;
        if (managedSettlement != this.settlement.isPresent()) {
            throw new IllegalArgumentException(
                    "Applied and not-applied managed results require a "
                            + "completed settlement; rejected and failed "
                            + "results cannot carry one"
            );
        }
        if (settlement != null
                && !request.lineage().equals(settlement.lineage())) {
            throw new IllegalArgumentException(
                    "Damage result settlement lineage must match its request"
            );
        }
    }

    public static DamageResult rejected(
            DamageRequest request,
            DamageFailureReason reason,
            String diagnostic
    ) {
        return new DamageResult(
                request,
                DamageSubmissionStatus.REJECTED,
                new DamageFailure(reason, diagnostic),
                false,
                false,
                0.0f,
                false,
                false,
                null
        );
    }

    public static DamageResult failed(
            DamageRequest request,
            DamageFailureReason reason,
            String diagnostic,
            boolean vanillaAccepted
    ) {
        return failed(
                request,
                reason,
                diagnostic,
                false,
                vanillaAccepted,
                0.0f,
                false,
                false
        );
    }

    public static DamageResult failed(
            DamageRequest request,
            DamageFailureReason reason,
            String diagnostic,
            boolean pipelineExecuted,
            boolean vanillaAccepted,
            float resolvedDamage,
            boolean critical,
            boolean cancelled
    ) {
        return new DamageResult(
                request,
                DamageSubmissionStatus.FAILED,
                new DamageFailure(reason, diagnostic),
                pipelineExecuted,
                vanillaAccepted,
                resolvedDamage,
                critical,
                cancelled,
                null
        );
    }

    @ApiStatus.Internal
    public static DamageResult fromSettlement(
            DamageRequest request,
            DamageSettlementSnapshot settlement,
            boolean vanillaAccepted
    ) {
        Objects.requireNonNull(settlement, "settlement");

        if (settlement.status() == DamageSettlementStatus.APPLIED) {
            return new DamageResult(
                    request,
                    DamageSubmissionStatus.APPLIED,
                    null,
                    settlement.pipelineExecuted(),
                    vanillaAccepted,
                    settlement.resolvedDamage(),
                    settlement.critical(),
                    settlement.cancelled(),
                    settlement
            );
        }

        DamageFailureReason reason = settlement.reason().orElseThrow();
        return new DamageResult(
                request,
                DamageSubmissionStatus.NOT_APPLIED,
                new DamageFailure(
                        reason,
                        "Managed damage settled without application: "
                                + reason
                ),
                settlement.pipelineExecuted(),
                vanillaAccepted,
                settlement.resolvedDamage(),
                settlement.critical(),
                settlement.cancelled(),
                settlement
        );
    }

    /**
     * Returns the caller's immutable request description. When a registered
     * resolver changes attribution, {@link #settlement()} contains the final
     * authoritative origin used by the damage source and pipeline.
     */
    public DamageRequest request() {
        return request;
    }

    public DamageLineage lineage() {
        return request.lineage();
    }

    public DamageSubmissionStatus status() {
        return status;
    }

    public Optional<DamageFailure> failure() {
        return failure;
    }

    public boolean pipelineExecuted() {
        return pipelineExecuted;
    }

    public boolean vanillaAccepted() {
        return vanillaAccepted;
    }

    public float resolvedDamage() {
        return resolvedDamage;
    }

    public boolean critical() {
        return critical;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public boolean applied() {
        return status == DamageSubmissionStatus.APPLIED;
    }

    /**
     * Completed managed settlement observation, absent for pre-pipeline
     * rejection/failure. It carries no child-request authority; only an exact
     * registered settlement callback may expose that capability.
     */
    public Optional<DamageSettlementSnapshot> settlement() {
        return settlement;
    }

    /** Final decision when this result carries a completed managed settlement. */
    public Optional<CriticalDecisionSnapshot> criticalDecision() {
        return settlement.map(DamageSettlementSnapshot::criticalDecision);
    }

    private static float sanitize(float amount) {
        return Float.isFinite(amount) ? Math.max(0.0f, amount) : 0.0f;
    }
}
