package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public record AttackerHealthAboveCondition(
        float threshold
) implements DamageRuleCondition {

    public static final MapCodec<AttackerHealthAboveCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.floatRange(0.0f, 1.0f)
                            .fieldOf("threshold")
                            .forGetter(AttackerHealthAboveCondition::threshold)
            ).apply(instance, AttackerHealthAboveCondition::new));

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.ATTACKER_HEALTH_ABOVE;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        if (!(ctx.logicalAttacker() instanceof LivingEntity livingAttacker)) {
            return false;
        }

        float max = livingAttacker.getMaxHealth();
        if (max <= 0.0f) {
            return false;
        }

        return livingAttacker.getHealth() / max > threshold;
    }
}
