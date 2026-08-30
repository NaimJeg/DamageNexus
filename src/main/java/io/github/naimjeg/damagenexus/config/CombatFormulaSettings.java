package io.github.naimjeg.damagenexus.config;

/**
 * Baked combat formula parameters. Resistance uses rating / (rating + K) for
 * non-negative ratings and rating / K for negative vulnerability ratings.
 */
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
