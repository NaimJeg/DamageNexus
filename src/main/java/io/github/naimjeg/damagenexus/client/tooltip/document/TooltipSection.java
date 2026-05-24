package io.github.naimjeg.damagenexus.client.tooltip.document;

public sealed interface TooltipSection permits
        AffixTooltipView,
        EntryTooltipView,
        VanillaTooltipAugmentation,
        DebugTooltipSection {
}
