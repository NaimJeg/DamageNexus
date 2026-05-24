package io.github.naimjeg.damagenexus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.naimjeg.damagenexus.bridge.vanilla.SpearDamageCapture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KineticWeapon.class)
public abstract class KineticWeaponMixin {

    private static final float EPSILON = 0.0001f;

    @WrapOperation(
            method = "damageEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;stabAttack(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z"
            )
    )
    private boolean damagenexus$captureKineticSpearAttack(
            LivingEntity attacker,
            EquipmentSlot slot,
            Entity target,
            float damageDealt,
            boolean dealsDamage,
            boolean dealsKnockback,
            boolean dismounts,
            Operation<Boolean> original,

            ItemStack stack,
            int ticksRemaining,
            LivingEntity livingEntity,
            EquipmentSlot equipmentSlot
    ) {
        KineticWeapon self = (KineticWeapon) (Object) this;

        Vec3 attackerLookVector = attacker.getLookAngle();

        double attackerSpeedProjection =
                attackerLookVector.dot(KineticWeapon.getMotion(attacker));

        double targetSpeedProjection =
                attackerLookVector.dot(KineticWeapon.getMotion(target));

        double relativeSpeed =
                Math.max(0.0D, attackerSpeedProjection - targetSpeedProjection);

        float computedSpeedBonus =
                (float) Mth.floor(relativeSpeed * (double) self.damageMultiplier());

        float rawBaseDamage = damageDealt - computedSpeedBonus;

        float attributeBaseDamage =
                (float) attacker.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);

        if (!Float.isFinite(rawBaseDamage)
                || rawBaseDamage < 0.0f
                || Math.abs(rawBaseDamage - attributeBaseDamage) > 0.01f) {
            rawBaseDamage = attributeBaseDamage;
            computedSpeedBonus = damageDealt - rawBaseDamage;
        }

        if (!Float.isFinite(computedSpeedBonus)
                || Math.abs(computedSpeedBonus) <= EPSILON) {
            computedSpeedBonus = 0.0f;
        }

        int ticksUsed =
                stack.getUseDuration(attacker)
                        - ticksRemaining
                        - self.delayTicks();

        SpearDamageCapture.captureCharge(
                attacker,
                target,
                stack,
                slot,

                rawBaseDamage,
                damageDealt,
                computedSpeedBonus,

                ticksUsed,
                attackerSpeedProjection,
                targetSpeedProjection,
                relativeSpeed,
                self.damageMultiplier(),

                dealsDamage,
                dealsKnockback,
                dismounts
        );

        /*
         * Do NOT clear here.
         *
         * LivingIncomingDamageEvent is the consumer. VanillaDamageCapture.clear()
         * and server-tick cleanup should own the lifetime.
         */
        return original.call(
                attacker,
                slot,
                target,
                damageDealt,
                dealsDamage,
                dealsKnockback,
                dismounts
        );
    }
}
