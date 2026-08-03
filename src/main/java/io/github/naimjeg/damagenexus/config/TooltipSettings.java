package io.github.naimjeg.damagenexus.config;

public record TooltipSettings(
        TooltipDebugLevel debugLevel
) {
    public static TooltipSettings defaults() {
        return new TooltipSettings(TooltipDebugLevel.OFF);
    }

    public boolean debugTooltipsEnabled() {
        return debugLevel != TooltipDebugLevel.OFF;
    }

    public boolean showAffixDebugTooltips() {
        return switch (debugLevel) {
            case OFF -> false;
            case SUMMARY, STRUCTURE, FULL -> true;
        };
    }

    public boolean showRuleDebugTooltips() {
        return switch (debugLevel) {
            case OFF, SUMMARY -> false;
            case STRUCTURE, FULL -> true;
        };
    }

    public boolean showFullTooltipTrace() {
        return debugLevel == TooltipDebugLevel.FULL;
    }
}
