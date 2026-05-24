package io.github.naimjeg.damagenexus.bridge.vanilla;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class SpearDamageCapture {

    private static final ThreadLocal<SpearFrame> CURRENT = new ThreadLocal<>();

    private SpearDamageCapture() {
    }

    public static void captureCharge(
            LivingEntity attacker,
            Entity target,
            ItemStack weapon,
            EquipmentSlot slot,

            float rawBaseDamage,
            float scaledDamage,
            float speedBonusDamage,

            int ticksUsed,
            double attackerSpeedProjection,
            double targetSpeedProjection,
            double relativeSpeed,
            float damageMultiplier,

            boolean dealsDamage,
            boolean dealsKnockback,
            boolean dismounts
    ) {
        CURRENT.set(new SpearFrame(
                SpearMode.CHARGE,
                attacker,
                target,
                weapon.copy(),
                slot,

                rawBaseDamage,
                scaledDamage,
                speedBonusDamage,

                ticksUsed,
                attackerSpeedProjection,
                targetSpeedProjection,
                relativeSpeed,
                damageMultiplier,

                dealsDamage,
                dealsKnockback,
                dismounts
        ));
    }

    public static @Nullable SpearFrame peekFor(Entity attacker, Entity victim) {
        SpearFrame frame = CURRENT.get();

        if (frame == null) {
            return null;
        }

        if (frame.attacker() != attacker) {
            return null;
        }

        if (frame.target() != victim) {
            return null;
        }

        return frame;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public enum SpearMode {
        STAB,
        CHARGE
    }

    public record SpearFrame(
            SpearMode mode,
            LivingEntity attacker,
            Entity target,
            ItemStack weapon,
            EquipmentSlot slot,

            float rawBaseDamage,
            float scaledDamage,
            float speedBonusDamage,

            int ticksUsed,
            double attackerSpeedProjection,
            double targetSpeedProjection,
            double relativeSpeed,
            float damageMultiplier,

            boolean dealsDamage,
            boolean dealsKnockback,
            boolean dismounts
    ) {
        public boolean hasSpeedBonus() {
            return Float.isFinite(speedBonusDamage)
                    && Math.abs(speedBonusDamage) > 0.0001f;
        }

        public float ratio() {
            if (!Float.isFinite(rawBaseDamage)
                    || Math.abs(rawBaseDamage) <= 0.0001f) {
                return 1.0f;
            }

            return scaledDamage / rawBaseDamage;
        }
    }
}
