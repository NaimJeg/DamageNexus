package io.github.naimjeg.damagenexus.mixin;

import io.github.naimjeg.damagenexus.bridge.vanilla.MaceDamageCapture;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MaceItem.class)
public abstract class MaceItemMixin {

    @Inject(
            method = "getAttackDamageBonus",
            at = @At("RETURN")
    )
    private void damagenexus$captureMaceAttackDamageBonus(
            Entity victim,
            float ignoredDamage,
            DamageSource damageSource,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!(damageSource.getDirectEntity() instanceof LivingEntity attacker)) {
            return;
        }

        float bonus = cir.getReturnValue();

        if (Math.abs(bonus) < 0.0001f) {
            return;
        }

        MaceDamageCapture.capture(
                attacker,
                victim,
                damageSource,
                (float) attacker.fallDistance,
                bonus
        );
    }
}
