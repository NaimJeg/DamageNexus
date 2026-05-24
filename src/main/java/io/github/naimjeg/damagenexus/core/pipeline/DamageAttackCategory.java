package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.DamageNexusTags;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageSourceProfile;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** Immutable internal attack classification fixed before pipeline execution. */
enum DamageAttackCategory {
    NONE,
    MELEE,
    PROJECTILE;

    static DamageAttackCategory classify(
            DamageSource source,
            VanillaDamageSourceProfile profile,
            @Nullable Entity authoritativeDirectEntity
    ) {
        return fromSignals(
                source.is(DamageTypeTags.IS_PROJECTILE),
                source.is(DamageNexusTags.DamageTypes.IS_PROJECTILE),
                source.is(DamageNexusTags.DamageTypes.IS_RANGED),
                source.getDirectEntity() instanceof Projectile,
                authoritativeDirectEntity instanceof Projectile,
                profile.projectile(),
                source.is(DamageNexusTags.DamageTypes.IS_MELEE),
                profile.shouldApplyMeleeOffensiveMobEffects()
        );
    }

    static DamageAttackCategory fromSignals(
            boolean vanillaProjectileTag,
            boolean nexusProjectileTag,
            boolean rangedTag,
            boolean directProjectile,
            boolean authoritativeDirectProjectile,
            boolean vanillaProfileProjectile,
            boolean meleeTag,
            boolean directPlayerOrMobAttack
    ) {
        boolean projectile = vanillaProjectileTag
                || nexusProjectileTag
                || rangedTag
                || directProjectile
                || authoritativeDirectProjectile
                || vanillaProfileProjectile;
        if (projectile) {
            return PROJECTILE;
        }
        return meleeTag || directPlayerOrMobAttack ? MELEE : NONE;
    }
}
