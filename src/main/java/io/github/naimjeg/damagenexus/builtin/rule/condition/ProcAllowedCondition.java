package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

/** Tests the current origin's frozen downstream PROC permission. */
public record ProcAllowedCondition() implements DamageRuleCondition {
    public static final MapCodec<ProcAllowedCondition> CODEC =
            MapCodec.unit(new ProcAllowedCondition());
    @Override public Identifier type() {
        return DamageNexusConditionIds.PROC_ALLOWED;
    }
    @Override public boolean test(DamageRuleContext ctx) {
        return ctx.origin().triggerPolicy().procAllowed();
    }
}
