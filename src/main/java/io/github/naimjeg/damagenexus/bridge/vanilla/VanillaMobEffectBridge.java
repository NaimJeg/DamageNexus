package io.github.naimjeg.damagenexus.bridge.vanilla;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class VanillaMobEffectBridge {

    private static final float EPSILON = 0.0001f;

    private VanillaMobEffectBridge() {
    }

    public static OffensiveMobEffectBreakdown computeOffensiveBreakdown(
            VanillaDamageSourceProfile profile
    ) {
        if (profile == null) {
            return OffensiveMobEffectBreakdown.NONE;
        }

        LivingEntity livingAttacker = profile.livingAttacker();

        if (livingAttacker == null) {
            return OffensiveMobEffectBreakdown.NONE;
        }

        if (!profile.shouldApplyMeleeOffensiveMobEffects()) {
            return OffensiveMobEffectBreakdown.NONE;
        }

        return computeBreakdownForLivingAttacker(livingAttacker);
    }

    private static OffensiveMobEffectBreakdown computeBreakdownForLivingAttacker(
            LivingEntity livingAttacker
    ) {
        AttributeInstance attackDamage =
                livingAttacker.getAttribute(Attributes.ATTACK_DAMAGE);

        if (attackDamage == null) {
            return OffensiveMobEffectBreakdown.NONE;
        }

        Set<Identifier> strengthModifierIds =
                collectAttackDamageModifierIds(
                        livingAttacker,
                        MobEffects.STRENGTH
                );

        Set<Identifier> weaknessModifierIds =
                collectAttackDamageModifierIds(
                        livingAttacker,
                        MobEffects.WEAKNESS
                );

        if (strengthModifierIds.isEmpty() && weaknessModifierIds.isEmpty()) {
            return OffensiveMobEffectBreakdown.NONE;
        }

        Set<Identifier> allMobEffectModifierIds = new HashSet<>();
        allMobEffectModifierIds.addAll(strengthModifierIds);
        allMobEffectModifierIds.addAll(weaknessModifierIds);

        Collection<AttributeModifier> currentModifiers =
                attackDamage.getModifiers();

        double currentValue = attackDamage.getValue();

        double valueWithoutBoth = calculateAttributeValue(
                Attributes.ATTACK_DAMAGE,
                attackDamage.getBaseValue(),
                currentModifiers,
                allMobEffectModifierIds
        );

        float observedDelta =
                finiteDelta(currentValue, valueWithoutBoth);

        /*
         * observedDelta is the authoritative value used for vanilla base
         * reconstruction.
         *
         * strengthDelta / weaknessDelta are debug-facing approximations. If a
         * mod changes Strength or Weakness to use multiplicative operations, the
         * exact split can be order-dependent, but observedDelta still remains
         * the value DN needs.
         */
        double valueWithoutWeakness = calculateAttributeValue(
                Attributes.ATTACK_DAMAGE,
                attackDamage.getBaseValue(),
                currentModifiers,
                weaknessModifierIds
        );

        float strengthDelta = strengthModifierIds.isEmpty()
                ? 0.0f
                : finiteDelta(valueWithoutWeakness, valueWithoutBoth);

        float weaknessDelta = weaknessModifierIds.isEmpty()
                ? 0.0f
                : observedDelta - strengthDelta;

        float enabledDelta = observedDelta;

        return new OffensiveMobEffectBreakdown(
                strengthDelta,
                weaknessDelta,
                observedDelta,
                enabledDelta
        );
    }

    private static Set<Identifier> collectAttackDamageModifierIds(
            LivingEntity attacker,
            Holder<MobEffect> effect
    ) {
        MobEffectInstance instance = attacker.getEffect(effect);

        if (instance == null) {
            return Set.of();
        }

        Set<Identifier> ids = new HashSet<>();

        effect.value().createModifiers(
                instance.getAmplifier(),
                (attribute, modifier) -> {
                    if (Attributes.ATTACK_DAMAGE.equals(attribute)) {
                        ids.add(modifier.id());
                    }
                }
        );

        return ids;
    }

    private static double calculateAttributeValue(
            Holder<Attribute> attribute,
            double baseValue,
            Collection<AttributeModifier> modifiers,
            Set<Identifier> excludedModifierIds
    ) {
        double base = baseValue;

        for (AttributeModifier modifier : modifiers) {
            if (excludedModifierIds.contains(modifier.id())) {
                continue;
            }

            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                base += modifier.amount();
            }
        }

        double result = base;

        for (AttributeModifier modifier : modifiers) {
            if (excludedModifierIds.contains(modifier.id())) {
                continue;
            }

            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                result += base * modifier.amount();
            }
        }

        for (AttributeModifier modifier : modifiers) {
            if (excludedModifierIds.contains(modifier.id())) {
                continue;
            }

            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                result *= 1.0D + modifier.amount();
            }
        }

        return attribute.value().sanitizeValue(result);
    }

    private static float finiteDelta(
            double valueWithModifier,
            double valueWithoutModifier
    ) {
        double delta = valueWithModifier - valueWithoutModifier;

        if (!Double.isFinite(delta)) {
            return 0.0f;
        }

        if (Math.abs(delta) <= EPSILON) {
            return 0.0f;
        }

        return (float) delta;
    }

    public record OffensiveMobEffectBreakdown(
            float strengthDelta,
            float weaknessDelta,
            float observedDelta,
            float enabledDelta
    ) {
        public static final OffensiveMobEffectBreakdown NONE =
                new OffensiveMobEffectBreakdown(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                );

        public boolean hasEnabledDelta() {
            return Math.abs(enabledDelta) > EPSILON;
        }

        public boolean hasObservedDelta() {
            return Math.abs(observedDelta) > EPSILON;
        }

        public boolean hasObservedWeaknessDelta() {
            return Math.abs(weaknessDelta) > EPSILON;
        }
    }
}
