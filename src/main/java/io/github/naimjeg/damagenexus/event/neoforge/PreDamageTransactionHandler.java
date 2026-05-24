package io.github.naimjeg.damagenexus.event.neoforge;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.core.trace.DamageNexusTransactionTracker;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementTracker;
import io.github.naimjeg.damagenexus.core.pipeline.DamageSourcePolicy;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = DamageNexus.MODID)
public final class PreDamageTransactionHandler {

    private PreDamageTransactionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        DamageSettlementTracker.capturePre(event);
        if (!DamageSourcePolicy.shouldManage(event.getSource())) {
            return;
        }
        DamageNexusTransactionTracker.promoteIncomingCandidate(
                event.getContainer(),
                event
        );
    }
}
