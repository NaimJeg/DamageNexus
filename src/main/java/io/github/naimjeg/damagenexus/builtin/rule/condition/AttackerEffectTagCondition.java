package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCodecs;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

public record AttackerEffectTagCondition(
        TagKey<MobEffect> tag
) implements DamageRuleCondition {

    public static final MapCodec<AttackerEffectTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.MOB_EFFECT_TAG
                            .fieldOf("tag")
                            .forGetter(AttackerEffectTagCondition::tag)
            ).apply(instance, AttackerEffectTagCondition::new));

    public AttackerEffectTagCondition {
        Objects.requireNonNull(tag, "tag");
    }

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.ATTACKER_EFFECT_TAG;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        LivingEntity attacker = ctx.logicalAttacker();
        if (attacker == null) return false;
        return MobEffectTagConditionSupport.matches(
                attacker.getActiveEffects(), tag
        );
    }
}
