package io.github.naimjeg.damagenexus.network;

import io.github.naimjeg.damagenexus.network.payload.DamageNumberPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DamageNexusNetwork {

    private DamageNexusNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        DamageNumberPayload.TYPE,
                        DamageNumberPayload.STREAM_CODEC
                );
    }
}
