package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

public record TargetHealthBelowCondition(
        float threshold
) implements DamageRuleCondition {

    public static final MapCodec<TargetHealthBelowCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.floatRange(0.0f, 1.0f)
                            .fieldOf("threshold")
                            .forGetter(TargetHealthBelowCondition::threshold)
            ).apply(instance, TargetHealthBelowCondition::new));

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.TARGET_HEALTH_BELOW;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        if (ctx.victim() == null) return false;

        float max = ctx.victim().getMaxHealth();
        if (max <= 0.0f) return false;

        return ctx.victim().getHealth() / max < threshold;
    }
}
