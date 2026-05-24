package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

public record HasParentDamageCondition() implements DamageRuleCondition {
    public static final MapCodec<HasParentDamageCondition> CODEC =
            MapCodec.unit(new HasParentDamageCondition());
    @Override public Identifier type() {
        return DamageNexusConditionIds.HAS_PARENT_DAMAGE;
    }
    @Override public boolean test(DamageRuleContext ctx) {
        return ctx.lineage().hasParent();
    }
}
