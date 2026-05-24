package io.github.naimjeg.damagenexus.core.trace;

import io.github.naimjeg.damagenexus.config.VanillaReductionCompatibilityMode;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public record DamageNexusTransaction(
        long damageId,
        LivingEntity attacker,
        LivingEntity victim,
        DamageSource source,

        float eventOriginalAmount,
        float initialBaseAmount,
        float offensiveTotal,
        float finalEventAmount,

        float eventAmountBeforeSet,
        float eventAmountAfterSet,

        float preNewDamage,
        float blockedDamage,
        float invulnerabilityReduction,
        float armorReduction,
        float enchantmentReduction,
        float mobEffectReduction,
        float innateResistanceReduction,
        boolean knownPreStageAdjustment,

        float victimHealthBefore,
        float victimAbsorptionBefore,
        int victimInvulnerableTimeBefore,
        long gameTime,

        VanillaReductionCompatibilityMode vanillaReductionMode,
        boolean suppressArmor,
        boolean suppressEnchantments,
        boolean suppressMobEffects,
        boolean suppressInnateResistance
) {
}
