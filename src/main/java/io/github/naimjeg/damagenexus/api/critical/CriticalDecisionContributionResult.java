package io.github.naimjeg.damagenexus.api.critical;

/** Result of contributing through a provider-scoped collector. */
public enum CriticalDecisionContributionResult {
    ACCEPTED,
    DUPLICATE,
    CONFLICT_RESOLVED_TO_SUPPRESS,
    REJECTED_DEFAULT;

    public String translationKey() {
        return "critical_contribution_result.damagenexus."
                + name().toLowerCase(java.util.Locale.ROOT);
    }
}
