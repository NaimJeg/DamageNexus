package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageSourceProfile;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Internal immutable construction payload for DamageNexusContext.
 *
 * <p>The factory owns how this spec is built. The context owns how the spec is
 * decomposed into event/source/diagnostics/packet/combat state.</p>
 */
record DamageNexusContextSpec(
        LivingIncomingDamageEvent event,
        LivingEntity attacker,
        LivingEntity victim,
        DamageOrigin origin,
        VanillaDamageSourceProfile sourceProfile,
        float initialBaseAmount,
        VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
        boolean rebuildVanillaOffensiveMobEffects,
        boolean rebuildVanillaOffensiveEnchantment,
        boolean rebuildVanillaPreEventDelta,
        float vanillaOffensiveMobEffectDelta,
        DamageApplicationBucket initialBaseBucket,
        DamageApplicationBucket vanillaOffensiveMobEffectBucket,
        DamageApplicationBucket vanillaOffensiveEnchantmentBucket
) {
    static DamageNexusContextSpec of(
            LivingIncomingDamageEvent event,
            LivingEntity attacker,
            LivingEntity victim,
            DamageOrigin origin,
            VanillaDamageSourceProfile sourceProfile,
            float initialBaseAmount,
            VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
            boolean rebuildVanillaOffensiveMobEffects,
            boolean rebuildVanillaOffensiveEnchantment,
            boolean rebuildVanillaPreEventDelta,
            float vanillaOffensiveMobEffectDelta,
            DamageApplicationBucket initialBaseBucket,
            DamageApplicationBucket vanillaOffensiveMobEffectBucket,
            DamageApplicationBucket vanillaOffensiveEnchantmentBucket
    ) {
        return new DamageNexusContextSpec(
                event,
                attacker,
                victim,
                origin,
                sourceProfile,
                initialBaseAmount,
                vanillaSnapshot,
                rebuildVanillaOffensiveMobEffects,
                rebuildVanillaOffensiveEnchantment,
                rebuildVanillaPreEventDelta,
                vanillaOffensiveMobEffectDelta,
                initialBaseBucket,
                vanillaOffensiveMobEffectBucket,
                vanillaOffensiveEnchantmentBucket
        );
    }
}

