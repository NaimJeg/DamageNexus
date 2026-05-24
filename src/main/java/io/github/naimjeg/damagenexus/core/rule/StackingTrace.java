package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import net.minecraft.resources.Identifier;

public record StackingTrace(
        DamagePhase phase,
        Identifier kept,
        Identifier dropped,
        DamageRuleStacking policy,
        float keptValue,
        float droppedValue
) {
}
