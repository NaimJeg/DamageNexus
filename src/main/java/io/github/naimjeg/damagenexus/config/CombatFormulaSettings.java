package io.github.naimjeg.damagenexus.config;

public record CombatFormulaSettings(
        float asymptoticKValue,
        float resistanceKValue,
        float ratingPerProtScore
) {
    public static CombatFormulaSettings defaults() {
        return new CombatFormulaSettings(
                15.0f,
                50.0f,
                3.5f
        );
    }
}
