package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;


public record TargetOnFireCondition() implements DamageRuleCondition {

    public static final MapCodec<TargetOnFireCondition> CODEC =
            MapCodec.unit(new TargetOnFireCondition());

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.TARGET_ON_FIRE;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return ctx.victim() != null && ctx.victim().isOnFire();
    }
}
