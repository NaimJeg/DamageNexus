package io.github.naimjeg.damagenexus.api.damage;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Immutable role assignment for a damage request.
 *
 * <p>Entity references are live server identities. The role assignment itself
 * is immutable and is validated again when submitted. A non-null equipment
 * owner is normally authorized only when it is the logical attacker. A
 * registered server-side resolver may establish a different owner after
 * independently verifying the proxy relationship.</p>
 */
public record DamageAttribution(
        @Nullable Entity directEntity,
        @Nullable LivingEntity logicalAttacker,
        @Nullable Entity effectOwner,
        @Nullable LivingEntity equipmentOwner
) {
    public static final DamageAttribution ENVIRONMENT =
            new DamageAttribution(null, null, null, null);

    /**
     * Resolves ordinary native attribution defaults. A missing direct entity
     * or effect owner falls back to the logical attacker. A missing equipment
     * owner falls back only to the logical attacker, never to another living
     * role.
     */
    public static DamageAttribution defaults(
            @Nullable Entity directEntity,
            @Nullable LivingEntity logicalAttacker,
            @Nullable Entity effectOwner,
            @Nullable LivingEntity equipmentOwner
    ) {
        Entity resolvedDirect = directEntity != null
                ? directEntity
                : logicalAttacker;
        Entity resolvedEffectOwner = effectOwner != null
                ? effectOwner
                : logicalAttacker;
        LivingEntity resolvedEquipmentOwner = equipmentOwner != null
                ? equipmentOwner
                : logicalAttacker;

        return new DamageAttribution(
                resolvedDirect,
                logicalAttacker,
                resolvedEffectOwner,
                resolvedEquipmentOwner
        );
    }
}
