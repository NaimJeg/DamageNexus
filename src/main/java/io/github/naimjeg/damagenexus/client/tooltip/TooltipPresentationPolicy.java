package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.config.TooltipDebugLevel;
import java.util.Objects;

public record TooltipPresentationPolicy(
        TooltipDetailLevel detailLevel,
        TooltipDebugLevel debugLevel
) {
    public TooltipPresentationPolicy {
        Objects.requireNonNull(detailLevel, "detailLevel");
        Objects.requireNonNull(debugLevel, "debugLevel");
    }
}
