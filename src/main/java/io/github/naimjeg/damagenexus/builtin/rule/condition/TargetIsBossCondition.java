package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.util.EntityConditionUtil;
import net.minecraft.resources.Identifier;

public record TargetIsBossCondition() implements DamageRuleCondition {

    public static final MapCodec<TargetIsBossCondition> CODEC =
            MapCodec.unit(new TargetIsBossCondition());

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.TARGET_IS_BOSS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return EntityConditionUtil.isBoss(ctx.victim());
    }
}
