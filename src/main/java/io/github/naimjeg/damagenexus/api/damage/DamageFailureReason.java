package io.github.naimjeg.damagenexus.api.damage;

/** Stable machine-readable reason for a rejected or unapplied request. */
public enum DamageFailureReason {
    NONE,
    DUPLICATE_REQUEST,
    ACTIVE_TRANSACTION,
    /** A child reference was used outside its exact registered callback. */
    PARENT_AUTHORITY_INACTIVE,
    /** A parentless root was submitted during settlement delivery. */
    ROOT_REQUEST_DURING_SETTLEMENT,
    WRONG_THREAD,
    TARGET_WRONG_LEVEL,
    TARGET_REMOVED,
    /** The target is not currently attached to its authoritative server level. */
    TARGET_NOT_ADDED,
    TARGET_DEAD,
    DIRECT_ENTITY_INVALID,
    LOGICAL_ATTACKER_INVALID,
    EFFECT_OWNER_INVALID,
    EQUIPMENT_OWNER_INVALID,
    EQUIPMENT_OWNER_UNAUTHORIZED,
    /** The completed parent does not authorize creation of a PROC child. */
    PROC_SUPPRESSED,
    /** The completed parent does not authorize a REFLECTED child. */
    REFLECTION_SUPPRESSED,
    /** The completed parent does not authorize a THORNS child. */
    THORNS_SUPPRESSED,
    /** The explicit lineage depth is greater than the configured maximum. */
    MAX_RECURSION_DEPTH,
    /** The shared root lineage consumed all derived-request slots. */
    ROOT_DERIVATION_LIMIT,
    /** The owning server consumed its managed admissions for this tick. */
    SERVER_TICK_BUDGET_EXHAUSTED,
    /** The lower-level event stack-safety fuse rejected re-entry. */
    EVENT_REENTRANCY_LIMIT,
    UNKNOWN_DAMAGE_TYPE,
    SOURCE_NOT_MANAGED,
    PIPELINE_NOT_OBSERVED,
    CANCELLED,
    LATE_INCOMING_CANCELLATION,
    ZERO_DAMAGE,
    VANILLA_REJECTED,
    INTERNAL_ERROR,
    ZERO_AFTER_PRE;

    public String translationKey() {
        return "damagenexus.damage_request.failure."
                + name().toLowerCase(java.util.Locale.ROOT);
    }
}
