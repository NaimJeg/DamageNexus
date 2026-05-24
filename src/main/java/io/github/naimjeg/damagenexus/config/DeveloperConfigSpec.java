package io.github.naimjeg.damagenexus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DeveloperConfigSpec {
    public static ModConfigSpec.BooleanValue ENABLE_TEST_COMMANDS;
    public static ModConfigSpec.BooleanValue STRICT_PROCESSOR_ERRORS;
    public static ModConfigSpec.BooleanValue STRICT_RULE_ERRORS;

    private DeveloperConfigSpec() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("developer");

        ENABLE_TEST_COMMANDS = builder
                .comment(
                        "Enable DamageNexus developer test commands.",
                        "This allows commands that can spawn test targets or generate test items.",
                        "Keep this disabled for normal gameplay and public builds.",
                        "Default: false"
                )
                .define("testCommandsEnabled", false);

        STRICT_PROCESSOR_ERRORS = builder
                .comment(
                        "If true, processor exceptions are rethrown and can crash the damage event.",
                        "Use true during development.",
                        "If false, failing processors are logged and skipped.",
                        "Default: false"
                )
                .define("strictProcessorErrors", false);

        STRICT_RULE_ERRORS = builder
                .comment(
                        "If true, rule condition/operation exceptions are rethrown.",
                        "Use true during development.",
                        "If false, failing rule conditions/operations are logged and skipped.",
                        "Default: false"
                )
                .define("strictRuleErrors", false);

        builder.pop();
    }

    static DeveloperSettings bake() {
        return new DeveloperSettings(
                ENABLE_TEST_COMMANDS.get(),
                STRICT_PROCESSOR_ERRORS.get(),
                STRICT_RULE_ERRORS.get()
        );
    }
}
