package io.github.naimjeg.damagenexus.config;

public record DamageNexusConfigValues(
        DeveloperSettings developer,
        DiagnosticsSettings diagnostics,
        TooltipSettings tooltips,
        CombatFormulaSettings formulas,
        VanillaCompatibilitySettings vanillaCompatibility,
        DamageSafetySettings safety
) {
    public static DamageNexusConfigValues defaults() {
        return new DamageNexusConfigValues(
                DeveloperSettings.defaults(),
                DiagnosticsSettings.defaults(),
                TooltipSettings.defaults(),
                CombatFormulaSettings.defaults(),
                VanillaCompatibilitySettings.defaults(),
                DamageSafetySettings.defaults()
        );
    }
}
