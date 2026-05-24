package io.github.naimjeg.damagenexus.client.tooltip;

import net.minecraft.network.chat.MutableComponent;

@FunctionalInterface
public interface RuleTooltipProvider<T> {

    MutableComponent describe(
            T value,
            RuleTooltipContext ctx,
            RuleTooltipMode mode
    );
}
