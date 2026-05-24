package io.github.naimjeg.damagenexus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DiagnosticsConfigSpec {
    public static ModConfigSpec.EnumValue<DiagnosticMode> DIAGNOSTIC_MODE;

    private DiagnosticsConfigSpec() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("diagnostics");

        DIAGNOSTIC_MODE = builder
                .comment(
                        "Primary DamageNexus diagnostics mode.",
                        "OFF: no transaction diagnostics or trace output; normal warnings may still be logged.",
                        "COMPATIBILITY: emit compatibility diagnostics for vanilla/other-mod interaction checks without normal trace spam.",
                        "SUMMARY: emit compact transaction summaries plus compatibility diagnostics.",
                        "FULL_TRACE: emit verbose processor, rule, mutation, contribution, and bucket-level trace details.",
                        "Default: OFF"
                )
                .defineEnum("mode", DiagnosticMode.OFF);
        builder.pop();
    }

    static DiagnosticsSettings bake() {
        return new DiagnosticsSettings(DIAGNOSTIC_MODE.get());
    }
}
