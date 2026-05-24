package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.DamageNexus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Releases per-server tick state when its owning server stops. */
@EventBusSubscriber(modid = DamageNexus.MODID)
final class DamageAdmissionLifecycle {

    private DamageAdmissionLifecycle() {
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DamageAdmissionController.clearServer(event.getServer());
    }
}
