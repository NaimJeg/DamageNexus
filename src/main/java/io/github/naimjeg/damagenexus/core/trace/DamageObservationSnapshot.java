package io.github.naimjeg.damagenexus.core.trace;

import net.minecraft.world.entity.LivingEntity;

public record DamageObservationSnapshot(
        float health,
        float absorption,
        int invulnerableTime,
        long gameTime
) {
    public static DamageObservationSnapshot capture(LivingEntity victim) {
        return new DamageObservationSnapshot(
                victim.getHealth(),
                victim.getAbsorptionAmount(),
                victim.invulnerableTime,
                victim.level().getGameTime()
        );
    }
}
