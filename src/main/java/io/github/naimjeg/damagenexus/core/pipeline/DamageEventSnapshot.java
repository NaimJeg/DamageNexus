package io.github.naimjeg.damagenexus.core.pipeline;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

record DamageEventSnapshot(
        LivingIncomingDamageEvent neoforgeEvent,
        LivingEntity attacker,
        LivingEntity victim,
        DamageSource source,
        float eventOriginalAmount,
        float initialBaseAmount
) {
    static DamageEventSnapshot create(
            LivingIncomingDamageEvent event,
            LivingEntity attacker,
            LivingEntity victim,
            float initialBaseAmount
    ) {
        float original = event.getOriginalAmount();

        float safeInitialBase =
                Float.isFinite(initialBaseAmount)
                        ? Math.max(0.0f, initialBaseAmount)
                        : Math.max(0.0f, original);

        return new DamageEventSnapshot(
                event,
                attacker,
                victim,
                event.getSource(),
                original,
                safeInitialBase
        );
    }
}
