package io.github.naimjeg.damagenexus.bridge.vanilla;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public final class MaceDamageCapture {

    private static final ThreadLocal<MaceFrame> CURRENT = new ThreadLocal<>();

    private MaceDamageCapture() {
    }

    public static void capture(
            LivingEntity attacker,
            Entity victim,
            DamageSource source,
            float fallDistance,
            float returnedBonus
    ) {
        CURRENT.set(new MaceFrame(
                attacker,
                victim,
                source,
                fallDistance,
                returnedBonus
        ));
    }

    public static @Nullable MaceFrame peekFor(
            DamageSource source,
            Entity victim
    ) {
        MaceFrame frame = CURRENT.get();

        if (frame == null) {
            return null;
        }

        if (frame.source() != source) {
            return null;
        }

        if (frame.victim() != victim) {
            return null;
        }

        return frame;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record MaceFrame(
            LivingEntity attacker,
            Entity victim,
            DamageSource source,
            float fallDistance,
            float returnedBonus
    ) {
    }
}
