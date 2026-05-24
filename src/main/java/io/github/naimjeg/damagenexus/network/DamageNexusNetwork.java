package io.github.naimjeg.damagenexus.network;

import io.github.naimjeg.damagenexus.network.payload.DamageNumberPayload;
import io.github.naimjeg.damagenexus.network.payload.DamageDummyApplyAttributesPayload;
import io.github.naimjeg.damagenexus.network.payload.DamageDummyAttributesPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DamageNexusNetwork {

    private DamageNexusNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .playToClient(
                        DamageNumberPayload.TYPE,
                        DamageNumberPayload.STREAM_CODEC
                )
                .playToServer(
                        DamageDummyApplyAttributesPayload.TYPE,
                        DamageDummyApplyAttributesPayload.STREAM_CODEC,
                        DamageDummyAttributePayloadHandler::handleApply
                )
                .playToClient(
                        DamageDummyAttributesPayload.TYPE,
                        DamageDummyAttributesPayload.STREAM_CODEC
                );
    }
}
