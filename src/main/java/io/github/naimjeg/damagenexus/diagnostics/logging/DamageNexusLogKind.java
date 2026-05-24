package io.github.naimjeg.damagenexus.diagnostics.logging;

public enum DamageNexusLogKind {
    /**
     * Full per-transaction trace:
     * PHASE, PROCESSOR_RUN, PROCESSOR_SKIP, RULE_SKIP, bucket details, mutations.
     * Server log only.
     */
    TRACE_DETAIL,

    /**
     * Compact transaction-level lines:
     * BEGIN, APPLY, POST observed, CANDIDATE_PROMOTE.
     */
    TRACE_SUMMARY,

    /**
     * Diagnostics used for mod compatibility investigation:
     * bypass suspicion, unmatched Post, vanilla adjustment, transaction lifecycle.
     */
    COMPATIBILITY
}
