package io.github.naimjeg.damagenexus.event.neoforge;

import io.github.naimjeg.damagenexus.DamageNexus;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = DamageNexus.MODID)
public final class VanillaCritHandler {

    private static final ThreadLocal<PendingVanillaCrit> PENDING_CRIT =
            new ThreadLocal<>();

    private VanillaCritHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVanillaCriticalHit(CriticalHitEvent event) {
        Player attacker = event.getEntity();
        Entity target = event.getTarget();

        if (event.isCriticalHit()
                && !attacker.level().isClientSide()) {
            float multiplier = event.getDamageMultiplier();
            if (!Float.isFinite(multiplier) || multiplier < 0.0f) {
                multiplier = 1.0f;
            }
            PENDING_CRIT.set(new PendingVanillaCrit(
                    attacker.getId(),
                    target.getId(),
                    attacker.level().getGameTime(),
                    true,
                    multiplier
            ));
        } else {
            clear();
        }

        /*
         * DN captures vanilla critical state here, then rebuilds the critical
         * bonus inside the DamageNexus pipeline.
         *
         * This prevents vanilla's critical multiplier from being applied before
         * DN reconstructs the offensive transaction.
         */
        event.setDamageMultiplier(1.0f);
    }

    public static @Nullable PendingVanillaCrit consumePendingVanillaCritical(
            @Nullable LivingEntity attacker,
            LivingEntity victim,
            DamageSource source
    ) {
        PendingVanillaCrit pending = PENDING_CRIT.get();

        if (pending == null) {
            return null;
        }

        if (!pending.matches(attacker, victim, source)) {
            clear();
            return null;
        }

        clear();
        return pending;
    }

    public static void clear() {
        PENDING_CRIT.remove();
    }

    public record PendingVanillaCrit(
            int attackerId,
            int targetId,
            long gameTime,
            boolean effectiveCritical,
            float multiplier
    ) {
        private static boolean isVanillaPlayerAttackSource(DamageSource source) {
            String msgId = source.type().msgId();

            return "player".equals(msgId)
                    || "player_attack".equals(msgId);
        }

        private boolean matches(
                @Nullable LivingEntity attacker,
                LivingEntity victim,
                DamageSource source
        ) {
            if (!effectiveCritical) {
                return false;
            }

            if (attacker == null) {
                return false;
            }

            if (attackerId >= 0 && attacker.getId() != attackerId) {
                return false;
            }

            if (victim.getId() != targetId) {
                return false;
            }

            if (gameTime >= 0L && attacker.level().getGameTime() != gameTime) {
                return false;
            }

            if (source.getEntity() != attacker) {
                return false;
            }

            if (source.getDirectEntity() != attacker) {
                return false;
            }

            return isVanillaPlayerAttackSource(source);
        }
    }
}
