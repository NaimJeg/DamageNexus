package io.github.naimjeg.damagenexus.diagnostics.logging;

public interface CombatTrace {

    boolean enabled();

    CombatTransactionLog transaction();

    CombatPipelineLog pipeline();

    CombatRuleLog rules();

    CombatMutationLog mutations();

    CombatCalculationLog calculation();

    CombatContributionLog contributions();

    /** Internal checkpoint for buffered successful mutation operations. */
    default int mutationCheckpoint() {
        return 0;
    }

    /** Removes buffered mutation operations recorded after a failed callback. */
    default void rollbackMutations(int checkpoint) {
    }
}
