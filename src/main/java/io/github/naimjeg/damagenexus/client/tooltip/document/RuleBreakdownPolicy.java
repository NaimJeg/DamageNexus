package io.github.naimjeg.damagenexus.client.tooltip.document;

public enum RuleBreakdownPolicy {
    NONE,
    DETAIL_ONLY,
    SUMMARY_AND_DETAIL;

    public static RuleBreakdownPolicy fromLegacy(boolean showRuleBreakdown) {
        return showRuleBreakdown ? SUMMARY_AND_DETAIL : NONE;
    }

    public boolean visibleInCompact() {
        return this == SUMMARY_AND_DETAIL;
    }

    public boolean visibleInDetail() {
        return this != NONE;
    }
}
