package io.github.naimjeg.damagenexus.bridge.vanilla;

import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.diagnostics.logging.VanillaBridgeDiagnosticsLog;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ProjectileDamageCapture {

    private static final ThreadLocal<ProjectileFrame> CURRENT =
            new ThreadLocal<>();

    private ProjectileDamageCapture() {
    }

    public static void capture(
            Entity projectile,
            Entity victim,
            DamageSource source,
            @Nullable ItemStack weapon,
            float preEnchantDamage,
            float postEnchantDamage,
            int preCritDamage,
            int postCritDamage,
            boolean critical
    ) {
        if (!Float.isFinite(preEnchantDamage)
                || !Float.isFinite(postEnchantDamage)) {
            return;
        }

        if (preCritDamage < 0 || postCritDamage < 0) {
            return;
        }

        CURRENT.set(new ProjectileFrame(
                projectile,
                victim,
                source,
                weapon == null ? ItemStack.EMPTY : weapon.copy(),
                Math.max(0.0f, preEnchantDamage),
                Math.max(0.0f, postEnchantDamage),
                Math.max(0, preCritDamage),
                Math.max(0, postCritDamage),
                critical,
                Math.max(0, postCritDamage - preCritDamage)
        ));

        if (DamageNexusSettings.fullTraceEnabled()) {
            VanillaBridgeDiagnosticsLog.projectileDamage(
                    weapon,
                    preEnchantDamage,
                    postEnchantDamage,
                    preCritDamage,
                    postCritDamage,
                    critical,
                    source
            );
        }
    }

    public static @Nullable ProjectileFrame peekFor(
            DamageSource source,
            Entity victim
    ) {
        ProjectileFrame frame = CURRENT.get();

        if (frame == null) {
            return null;
        }

        if (frame.victim() != victim) {
            return null;
        }

        if (frame.source() != source) {
            return null;
        }

        return frame;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record ProjectileFrame(
            Entity projectile,
            Entity victim,
            DamageSource source,
            ItemStack weapon,
            float preEnchantDamage,
            float postEnchantDamage,
            int preCritDamage,
            int postCritDamage,
            boolean critical,
            int criticalBonus
    ) {
    }
}

