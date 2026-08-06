package io.github.naimjeg.damagenexus.client.tooltip.document;

public enum RuleBreakdownPolicy {
    NONE,
    DETAIL_ONLY,
    SUMMARY_AND_DETAIL;

    public static RuleBreakdownPolicy fromDisplay(
            boolean showRuleBreakdown,
            boolean hasAuthoredSummary
    ) {
        if (!showRuleBreakdown) {
            return NONE;
        }
        return hasAuthoredSummary ? DETAIL_ONLY : SUMMARY_AND_DETAIL;
    }

    /**
     * @deprecated Use {@link #fromDisplay(boolean, boolean)}.
     */
    @Deprecated(forRemoval = false)
    public static RuleBreakdownPolicy fromLegacy(boolean showRuleBreakdown) {
        return fromDisplay(showRuleBreakdown, false);
    }

    public boolean visibleInCompact() {
        return this == SUMMARY_AND_DETAIL;
    }

    public boolean visibleInDetail() {
        return this != NONE;
    }
}
