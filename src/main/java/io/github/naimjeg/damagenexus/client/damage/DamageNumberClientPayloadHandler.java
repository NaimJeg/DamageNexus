package io.github.naimjeg.damagenexus.client.damage;

import io.github.naimjeg.damagenexus.network.payload.DamageNumberPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class DamageNumberClientPayloadHandler {

    private DamageNumberClientPayloadHandler() {
    }

    public static void onRegisterPayloadHandlers(
            RegisterClientPayloadHandlersEvent event
    ) {
        event.register(
                DamageNumberPayload.TYPE,
                DamageNumberClientPayloadHandler::handle
        );
    }

    public static void handle(
            DamageNumberPayload payload,
            IPayloadContext context
    ) {
        ClientDamageNumberManager.spawn(payload);
    }
}
