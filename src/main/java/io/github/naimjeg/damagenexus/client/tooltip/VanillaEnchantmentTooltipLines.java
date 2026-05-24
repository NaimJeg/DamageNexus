package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.client.phrase.NumberValue;
import io.github.naimjeg.damagenexus.api.client.phrase.PercentValue;
import io.github.naimjeg.damagenexus.client.tooltip.document.VanillaTooltipNote;

import java.util.List;

public final class VanillaEnchantmentTooltipLines {
    private VanillaEnchantmentTooltipLines() {
    }

    public static VanillaTooltipNote featherFallingEpf(float epf) {
        return note(
                "tooltip.damagenexus.vanilla_enchantment.feather_falling.epf",
                new NumberValue(epf)
        );
    }

    public static VanillaTooltipNote featherFallingResistance(float rating) {
        return note(
                "tooltip.damagenexus.vanilla_enchantment.feather_falling.resistance",
                new NumberValue(rating)
        );
    }

    public static VanillaTooltipNote breachReduction(float reduction) {
        return note(
                "tooltip.damagenexus.vanilla_enchantment.breach.reduction",
                new PercentValue(reduction)
        );
    }

    public static VanillaTooltipNote powerFormula() {
        return note("tooltip.damagenexus.vanilla_enchantment.power.formula");
    }

    public static VanillaTooltipNote densityPerBlock(float damagePerBlock) {
        return note(
                "tooltip.damagenexus.vanilla_enchantment.density.per_block",
                new NumberValue(damagePerBlock)
        );
    }

    public static VanillaTooltipNote densityFormula() {
        return note("tooltip.damagenexus.vanilla_enchantment.density.formula");
    }

    private static VanillaTooltipNote note(
            String key,
            io.github.naimjeg.damagenexus.api.client.phrase.PhraseValue... arguments
    ) {
        return new VanillaTooltipNote(key, List.of(arguments));
    }
}
