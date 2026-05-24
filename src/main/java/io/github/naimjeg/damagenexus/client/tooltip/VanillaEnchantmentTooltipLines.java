package io.github.naimjeg.damagenexus.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public final class VanillaEnchantmentTooltipLines {

    private VanillaEnchantmentTooltipLines() {
    }

    public static MutableComponent featherFallingEpf(float epf) {
        return Component.translatable(
                "tooltip.damagenexus.vanilla_enchantment.feather_falling.epf",
                number(epf)
        );
    }

    public static MutableComponent featherFallingResistance(float rating) {
        return Component.translatable(
                "tooltip.damagenexus.vanilla_enchantment.feather_falling.resistance",
                number(rating)
        );
    }

    public static MutableComponent breachReduction(float reduction) {
        return Component.translatable(
                "tooltip.damagenexus.vanilla_enchantment.breach.reduction",
                percent(reduction)
        );
    }

    public static MutableComponent powerFormula() {
        return Component.translatable(
                "tooltip.damagenexus.vanilla_enchantment.power.formula"
        );
    }

    public static MutableComponent densityPerBlock(float damagePerBlock) {
        return Component.translatable(
                "tooltip.damagenexus.vanilla_enchantment.density.per_block",
                number(damagePerBlock)
        );
    }

    public static MutableComponent densityFormula() {
        return Component.translatable(
                "tooltip.damagenexus.vanilla_enchantment.density.formula"
        );
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String percent(float value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0f);
    }
}