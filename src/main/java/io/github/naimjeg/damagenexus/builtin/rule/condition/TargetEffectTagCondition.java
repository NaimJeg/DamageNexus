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

import java.util.Objects;

public record TargetEffectTagCondition(
        TagKey<MobEffect> tag
) implements DamageRuleCondition {

    public static final MapCodec<TargetEffectTagCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.MOB_EFFECT_TAG
                            .fieldOf("tag")
                            .forGetter(TargetEffectTagCondition::tag)
            ).apply(instance, TargetEffectTagCondition::new));

    public TargetEffectTagCondition {
        Objects.requireNonNull(tag, "tag");
    }

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.TARGET_EFFECT_TAG;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return MobEffectTagConditionSupport.matches(
                ctx.victim().getActiveEffects(), tag
        );
    }
}
