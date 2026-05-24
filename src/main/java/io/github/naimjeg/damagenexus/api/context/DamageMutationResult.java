package io.github.naimjeg.damagenexus.api.context;

public enum DamageMutationResult {
    APPLIED(true, false),

    NO_OP_ZERO(false, false),
    NO_OP_EMPTY_SOURCE(false, false),

    REJECTED_WRONG_PHASE(false, true),
    REJECTED_OFFENSE_LOCKED(false, true),
    REJECTED_DEFENSE_LOCKED(false, true),
    REJECTED_NON_FINITE(false, true),
    REJECTED_INVALID_BUCKET(false, true),
    REJECTED_INVALID_PRE_MULTIPLIER_BUCKET(false, true),
    REJECTED_INACTIVE_CHANNEL(false, true),
    REJECTED_EMPTY_SOURCE_ID(false, true),
    REJECTED_NULL_CHANNEL(false, true),
    REJECTED_NULL_APPLICATION_BUCKET(false, true),
    REJECTED_FINAL_DEFENSE_NOT_CALCULATED(false, true),
    REJECTED_RULE_EXCEPTION(false, true);

    private final boolean applied;
    private final boolean rejected;

    DamageMutationResult(
            boolean applied,
            boolean rejected
    ) {
        this.applied = applied;
        this.rejected = rejected;
    }

    public boolean applied() {
        return applied;
    }

    public boolean rejected() {
        return rejected;
    }

    public boolean noOp() {
        return !applied && !rejected;
    }
}
