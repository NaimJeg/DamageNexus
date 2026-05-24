package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

public record IsProcDamageCondition() implements DamageRuleCondition {
    public static final MapCodec<IsProcDamageCondition> CODEC =
            MapCodec.unit(new IsProcDamageCondition());
    @Override public Identifier type() {
        return DamageNexusConditionIds.IS_PROC_DAMAGE;
    }
    @Override public boolean test(DamageRuleContext ctx) {
        return ctx.requestKind() == DamageRequestKind.PROC;
    }
}
