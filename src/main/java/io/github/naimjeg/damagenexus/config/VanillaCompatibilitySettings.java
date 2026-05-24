package io.github.naimjeg.damagenexus.config;

public record VanillaCompatibilitySettings(
        VanillaReductionCompatibilityMode mode,
        boolean rawSuppressArmor,
        boolean rawSuppressEnchantments,
        boolean rawSuppressMobEffects,
        boolean rawSuppressInnateResistance
) {
    public static VanillaCompatibilitySettings defaults() {
        return new VanillaCompatibilitySettings(
                VanillaReductionCompatibilityMode.FULL_REPLACEMENT,
                true,
                true,
                true,
                true
        );
    }

    public boolean shouldSuppressArmor() {
        return switch (mode) {
            case FULL_REPLACEMENT -> true;
            case COOPERATIVE -> false;
            case CONFIGURABLE -> rawSuppressArmor;
        };
    }

    public boolean shouldSuppressEnchantments() {
        return switch (mode) {
            case FULL_REPLACEMENT -> true;
            case COOPERATIVE -> false;
            case CONFIGURABLE -> rawSuppressEnchantments;
        };
    }

    public boolean shouldSuppressMobEffects() {
        return switch (mode) {
            case FULL_REPLACEMENT -> true;
            case COOPERATIVE -> false;
            case CONFIGURABLE -> rawSuppressMobEffects;
        };
    }

    public boolean shouldSuppressInnateResistance() {
        return switch (mode) {
            case FULL_REPLACEMENT -> true;
            case COOPERATIVE -> false;
            case CONFIGURABLE -> rawSuppressInnateResistance;
        };
    }
}
