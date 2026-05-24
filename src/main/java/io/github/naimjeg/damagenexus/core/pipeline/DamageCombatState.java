package io.github.naimjeg.damagenexus.core.pipeline;

/**
 * Mutable per-transaction combat flags that are not part of the damage packet
 * math, source classification, event writeback, or diagnostics lifecycle.
 */
final class DamageCombatState {

    private final CriticalDecisionState criticalDecision = new CriticalDecisionState();
    private boolean attributeScalingApplied;
    private boolean armorHandled = false;
    private float armorEffectivenessMultiplier = 1.0f;

    Checkpoint checkpoint() {
        return new Checkpoint(
                criticalDecision.checkpoint(),
                attributeScalingApplied,
                armorHandled,
                armorEffectivenessMultiplier
        );
    }

    void restore(Checkpoint checkpoint) {
        this.criticalDecision.restore(checkpoint.criticalDecision);
        this.attributeScalingApplied = checkpoint.attributeScalingApplied;
        this.armorHandled = checkpoint.armorHandled;
        this.armorEffectivenessMultiplier =
                checkpoint.armorEffectivenessMultiplier;
    }

    void markCritical() {
        throw new IllegalStateException("Critical state is owned by the decision lifecycle");
    }

    boolean critical() {
        return criticalDecision.critical();
    }

    CriticalDecisionState criticalDecision() {
        return criticalDecision;
    }

    boolean claimAttributeScaling() {
        if (attributeScalingApplied) {
            return false;
        }
        attributeScalingApplied = true;
        return true;
    }

    boolean attributeScalingApplied() {
        return attributeScalingApplied;
    }

    void markArmorHandled() {
        this.armorHandled = true;
    }

    boolean armorHandled() {
        return armorHandled;
    }

    float armorEffectivenessMultiplier() {
        return armorEffectivenessMultiplier;
    }

    void multiplyArmorEffectiveness(float multiplier) {
        float safeMultiplier = Float.isFinite(multiplier)
                ? Math.max(0.0f, multiplier)
                : 0.0f;

        armorEffectivenessMultiplier *= safeMultiplier;

        if (!Float.isFinite(armorEffectivenessMultiplier)) {
            armorEffectivenessMultiplier = 0.0f;
        }

        armorEffectivenessMultiplier =
                Math.max(0.0f, armorEffectivenessMultiplier);
    }

    record Checkpoint(
            CriticalDecisionState.Checkpoint criticalDecision,
            boolean attributeScalingApplied,
            boolean armorHandled,
            float armorEffectivenessMultiplier
    ) {
    }
}

