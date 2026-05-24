package io.github.naimjeg.damagenexus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TooltipConfigSpec {
    public static ModConfigSpec.EnumValue<TooltipDebugLevel> TOOLTIP_DEBUG_LEVEL;

    private TooltipConfigSpec() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("tooltips");

        TOOLTIP_DEBUG_LEVEL = builder
                .comment(
                        "Controls DamageNexus debug information shown in item tooltips.",
                        "OFF: no debug tooltip sections.",
                        "AFFIX_SUMMARY: show affix-level debug sections only.",
                        "AFFIX_AND_RULES: show affixes and contained rule ids.",
                        "FULL: show full debug details.",
                        "Default: OFF"
                )
                .defineEnum(
                        "debugLevel",
                        TooltipDebugLevel.OFF
                );

        builder.pop();
    }

    static TooltipSettings bake() {
        return new TooltipSettings(
                TOOLTIP_DEBUG_LEVEL.get()
        );
    }
}
