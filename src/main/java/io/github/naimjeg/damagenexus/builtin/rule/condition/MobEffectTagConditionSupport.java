package io.github.naimjeg.damagenexus.builtin.rule.condition;

import net.minecraft.tags.TagKey;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

final class MobEffectTagConditionSupport {
    private MobEffectTagConditionSupport() {
    }

    static boolean matches(
            Iterable<MobEffectInstance> effects,
        TagKey<MobEffect> tag
    ) {
        for (MobEffectInstance instance : effects) {
            if (matchesHolder(instance.getEffect(), tag)) return true;
        }
        return false;
    }

    static boolean matchesHolder(
            Holder<MobEffect> effect,
            TagKey<MobEffect> tag
    ) {
        return effect.is(tag);
    }
}
