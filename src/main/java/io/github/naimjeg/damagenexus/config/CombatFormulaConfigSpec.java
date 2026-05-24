package io.github.naimjeg.damagenexus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CombatFormulaConfigSpec {
    public static ModConfigSpec.DoubleValue ASYMPTOTIC_K_VALUE;
    public static ModConfigSpec.DoubleValue RESISTANCE_K_VALUE;
    public static ModConfigSpec.DoubleValue RATING_PER_PROT_SCORE;

    private CombatFormulaConfigSpec() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("combatFormulas");

        ASYMPTOTIC_K_VALUE = builder
                .comment(
                        "K value for the asymptotic armor formula.",
                        "Formula: Reduction = effectiveArmor / (effectiveArmor + K).",
                        "Lower values make armor strong earlier. Higher values stretch progression.",
                        "Default: 15.0"
                )
                .defineInRange(
                        "asymptoticKValue",
                        15.0D,
                        1.0D,
                        1000.0D
                );

        RESISTANCE_K_VALUE = builder
                .comment(
                        "K value for the elemental resistance formula.",
                        "Formula: Reduction = rating / (rating + K).",
                        "Default: 50.0"
                )
                .defineInRange(
                        "resistanceKValue",
                        50.0D,
                        1.0D,
                        1000.0D
                );

        RATING_PER_PROT_SCORE = builder
                .comment(
                        "How many resistance rating points are granted per 1 point of vanilla Protection score.",
                        "Vanilla Protection IV grants 4 score.",
                        "If this is 3.5, Protection IV grants 14 rating per piece.",
                        "Default: 3.5"
                )
                .defineInRange(
                        "ratingPerProtScore",
                        3.5D,
                        0.0D,
                        100.0D
                );

        builder.pop();
    }

    static CombatFormulaSettings bake() {
        return new CombatFormulaSettings(
                ASYMPTOTIC_K_VALUE.get().floatValue(),
                RESISTANCE_K_VALUE.get().floatValue(),
                RATING_PER_PROT_SCORE.get().floatValue()
        );
    }
}
