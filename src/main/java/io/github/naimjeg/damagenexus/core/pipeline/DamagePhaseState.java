package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;

public final class DamagePhaseState {

    private DamagePhase currentPhase = DamagePhase.BASE_MODIFICATION;

    private boolean offensiveLocked = false;
    private boolean defensiveLocked = false;
    private boolean defenseCalculated = false;

    Checkpoint checkpoint() {
        return new Checkpoint(
                currentPhase,
                offensiveLocked,
                defensiveLocked,
                defenseCalculated
        );
    }

    void restore(Checkpoint checkpoint) {
        currentPhase = checkpoint.currentPhase;
        offensiveLocked = checkpoint.offensiveLocked;
        defensiveLocked = checkpoint.defensiveLocked;
        defenseCalculated = checkpoint.defenseCalculated;
    }

    public DamagePhase currentPhase() {
        return currentPhase;
    }

    void setCurrentPhase(DamagePhase phase) {
        this.currentPhase = phase;
    }

    public boolean offensiveLocked() {
        return offensiveLocked;
    }

    public boolean defensiveLocked() {
        return defensiveLocked;
    }

    public boolean defenseCalculated() {
        return defenseCalculated;
    }

    DamageMutationResult canModifyOffense() {
        return offensiveLocked
                ? DamageMutationResult.REJECTED_OFFENSE_LOCKED
                : DamageMutationResult.APPLIED;
    }

    DamageMutationResult canModifyDefense() {
        return defensiveLocked
                ? DamageMutationResult.REJECTED_DEFENSE_LOCKED
                : DamageMutationResult.APPLIED;
    }

    DamageMutationResult requirePhase(DamagePhase expected) {
        return currentPhase == expected
                ? DamageMutationResult.APPLIED
                : DamageMutationResult.REJECTED_WRONG_PHASE;
    }

    DamageMutationResult requireMultiplierPhase() {
        return switch (currentPhase) {
            case TYPE_SCALING,
                 CRITICAL_HIT,
                 CONDITIONAL_MULTI,
                 GLOBAL_ADJUSTMENT -> DamageMutationResult.APPLIED;

            default -> DamageMutationResult.REJECTED_WRONG_PHASE;
        };
    }

    DamageMutationResult requireGlobalPostMultiplierPhase() {
        return switch (currentPhase) {
            case CONDITIONAL_MULTI,
                 GLOBAL_ADJUSTMENT -> DamageMutationResult.APPLIED;

            default -> DamageMutationResult.REJECTED_WRONG_PHASE;
        };
    }

    void lockOffense() {
        this.offensiveLocked = true;
    }

    void markDefenseCalculatedAndLocked() {
        this.defenseCalculated = true;
        this.defensiveLocked = true;
    }

    void lockDefense() {
        this.defensiveLocked = true;
    }

    record Checkpoint(
            DamagePhase currentPhase,
            boolean offensiveLocked,
            boolean defensiveLocked,
            boolean defenseCalculated
    ) {
    }
}
