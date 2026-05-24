package io.github.naimjeg.damagenexus.mixin;

import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(
            method = "modifyDamage",
            at = @At("RETURN")
    )
    private static void damagenexus$captureModifyDamage(
            ServerLevel serverLevel,
            ItemStack itemStack,
            Entity victim,
            DamageSource damageSource,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        VanillaDamageCapture.captureModifyDamage(
                serverLevel,
                itemStack,
                victim,
                damageSource,
                damage,
                cir.getReturnValue()
        );
    }
}
