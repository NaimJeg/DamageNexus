package io.github.naimjeg.damagenexus.presentation.damage;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.damagenexus.api.damage.DamageSettlementStatus;
import io.github.naimjeg.damagenexus.api.event.DamageSettledEvent;
import io.github.naimjeg.damagenexus.network.payload.DamageNumberPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DamageNexus.MODID)
public final class DamageNumberBroadcaster {

    private static final float MIN_PRESENTED_DAMAGE = 0.001F;

    private DamageNumberBroadcaster() {
    }

    @SubscribeEvent
    public static void onDamageSettled(DamageSettledEvent event) {
        if (event == null) {
            return;
        }
        broadcast(event.snapshot());
    }

    private static void broadcast(DamageSettlementSnapshot snapshot) {
        if (snapshot.status() != DamageSettlementStatus.APPLIED) {
            return;
        }

        float damage = snapshot.appliedDamage();
        if (!Float.isFinite(damage) || damage <= MIN_PRESENTED_DAMAGE) {
            return;
        }

        if (!(snapshot.logicalAttacker() instanceof ServerPlayer player)) {
            return;
        }

        if (player.isRemoved() || player.hasDisconnected()) {
            return;
        }
        if (!player.connection.hasChannel(DamageNumberPayload.TYPE)) {
            return;
        }

        LivingEntity target = snapshot.target();
        if (target.level() != snapshot.level()) {
            return;
        }

        double y = target.getY() + target.getBbHeight() * 0.65D;
        DamageNumberPayload payload = new DamageNumberPayload(
                snapshot.damageId(),
                target.getX(),
                y,
                target.getZ(),
                damage,
                snapshot.critical()
        );

        try {
            PacketDistributor.sendToPlayer(player, payload);
        } catch (RuntimeException exception) {
            DamageNexus.LOGGER.warn(
                    "Failed to send DamageNexus damage-number payload to {}",
                    player.getGameProfile().name(),
                    exception
            );
        }
    }
}
