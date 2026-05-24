package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

public record AttackerHealthBelowCondition(
        float threshold
) implements DamageRuleCondition {

    public static final MapCodec<AttackerHealthBelowCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.floatRange(0.0f, 1.0f)
                            .fieldOf("threshold")
                            .forGetter(AttackerHealthBelowCondition::threshold)
            ).apply(instance, AttackerHealthBelowCondition::new));

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.ATTACKER_HEALTH_BELOW;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        if (ctx.logicalAttacker() == null) {
            return false;
        }

        float maxHealth = ctx.logicalAttacker().getMaxHealth();

        if (maxHealth <= 0.0f) {
            return false;
        }

        float ratio = ctx.logicalAttacker().getHealth() / maxHealth;
        return ratio < threshold;
    }
}
