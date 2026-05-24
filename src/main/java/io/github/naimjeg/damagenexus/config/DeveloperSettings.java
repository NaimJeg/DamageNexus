package io.github.naimjeg.damagenexus.config;

public record DeveloperSettings(
        boolean testCommandsEnabled,
        boolean strictProcessorErrors,
        boolean strictRuleErrors
) {
    public static DeveloperSettings defaults() {
        return new DeveloperSettings(
                false,
                false,
                false
        );
    }
}
