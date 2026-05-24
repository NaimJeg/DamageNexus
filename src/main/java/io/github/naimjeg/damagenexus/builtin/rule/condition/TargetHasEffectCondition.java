package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public record TargetHasEffectCondition(
        Identifier effect
) implements DamageRuleCondition {

    public static final MapCodec<TargetHasEffectCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC
                            .fieldOf("effect")
                            .forGetter(TargetHasEffectCondition::effect)
            ).apply(instance, TargetHasEffectCondition::new));

    private static Optional<Holder.Reference<MobEffect>> resolveEffect(
            LivingEntity entity,
            Identifier effectId
    ) {
        Registry<MobEffect> registry =
                entity.level()
                        .registryAccess()
                        .lookupOrThrow(Registries.MOB_EFFECT);

        ResourceKey<MobEffect> key =
                ResourceKey.create(Registries.MOB_EFFECT, effectId);

        return registry.get(key);
    }

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.TARGET_HAS_EFFECT;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        if (ctx.victim() == null) {
            return false;
        }

        Optional<Holder.Reference<MobEffect>> holder =
                resolveEffect(ctx.victim(), effect);

        return holder.isPresent()
                && ctx.victim().hasEffect(holder.get());
    }
}
