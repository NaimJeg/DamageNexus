package io.github.naimjeg.damagenexus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VanillaCompatibilityConfigSpec {
    public static ModConfigSpec.EnumValue<VanillaReductionCompatibilityMode> VANILLA_REDUCTION_COMPATIBILITY_MODE;
    public static ModConfigSpec.BooleanValue SUPPRESS_VANILLA_ARMOR_REDUCTION;
    public static ModConfigSpec.BooleanValue SUPPRESS_VANILLA_ENCHANTMENT_REDUCTION;
    public static ModConfigSpec.BooleanValue SUPPRESS_VANILLA_MOB_EFFECT_REDUCTION;
    public static ModConfigSpec.BooleanValue SUPPRESS_VANILLA_INNATE_RESISTANCE_REDUCTION;

    private VanillaCompatibilityConfigSpec() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("vanillaCompatibility");

        VANILLA_REDUCTION_COMPATIBILITY_MODE = builder
                .comment(
                        "Controls how DamageNexus interacts with vanilla reduction modifiers.",
                        "FULL_REPLACEMENT: suppress all vanilla reduction modifiers after DN mitigation.",
                        "CONFIGURABLE: use the four per-reduction booleans below.",
                        "COOPERATIVE: suppress no vanilla reduction modifiers. May cause double mitigation.",
                        "Default: FULL_REPLACEMENT"
                )
                .defineEnum(
                        "reductionMode",
                        VanillaReductionCompatibilityMode.FULL_REPLACEMENT
                );

        SUPPRESS_VANILLA_ARMOR_REDUCTION = builder
                .comment(
                        "Used only when reductionMode=CONFIGURABLE.",
                        "If true, DamageNexus suppresses vanilla ARMOR reduction after calculating DN mitigation.",
                        "Default: true"
                )
                .define("suppressArmorReduction", true);

        SUPPRESS_VANILLA_ENCHANTMENT_REDUCTION = builder
                .comment(
                        "Used only when reductionMode=CONFIGURABLE.",
                        "If true, DamageNexus suppresses vanilla ENCHANTMENTS reduction after calculating DN mitigation.",
                        "Default: true"
                )
                .define("suppressEnchantmentReduction", true);

        SUPPRESS_VANILLA_MOB_EFFECT_REDUCTION = builder
                .comment(
                        "Used only when reductionMode=CONFIGURABLE.",
                        "If true, DamageNexus suppresses vanilla MOB_EFFECTS reduction after calculating DN mitigation.",
                        "Default: true"
                )
                .define("suppressMobEffectReduction", true);

        SUPPRESS_VANILLA_INNATE_RESISTANCE_REDUCTION = builder
                .comment(
                        "Used only when reductionMode=CONFIGURABLE.",
                        "If true, DamageNexus suppresses vanilla INNATE_RESISTANCE reduction after calculating DN mitigation.",
                        "Default: true"
                )
                .define("suppressInnateResistanceReduction", true);

        builder.pop();
    }

    static VanillaCompatibilitySettings bake() {
        return new VanillaCompatibilitySettings(
                VANILLA_REDUCTION_COMPATIBILITY_MODE.get(),
                SUPPRESS_VANILLA_ARMOR_REDUCTION.get(),
                SUPPRESS_VANILLA_ENCHANTMENT_REDUCTION.get(),
                SUPPRESS_VANILLA_MOB_EFFECT_REDUCTION.get(),
                SUPPRESS_VANILLA_INNATE_RESISTANCE_REDUCTION.get()
        );
    }
}
