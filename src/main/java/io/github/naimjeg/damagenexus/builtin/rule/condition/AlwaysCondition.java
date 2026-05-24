package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

public record AlwaysCondition() implements DamageRuleCondition {

    public static final MapCodec<AlwaysCondition> CODEC =
            MapCodec.unit(new AlwaysCondition());

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.ALWAYS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return true;
    }
}
