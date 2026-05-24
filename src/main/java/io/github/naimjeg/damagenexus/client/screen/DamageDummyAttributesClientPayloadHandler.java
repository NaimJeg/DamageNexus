package io.github.naimjeg.damagenexus.client.screen;

import io.github.naimjeg.damagenexus.menu.DamageDummyMenu;
import io.github.naimjeg.damagenexus.network.payload.DamageDummyAttributesPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Physical-client-only receiver for authoritative menu snapshots. */
public final class DamageDummyAttributesClientPayloadHandler {

    private DamageDummyAttributesClientPayloadHandler() {
    }

    public static void onRegisterPayloadHandlers(
            RegisterClientPayloadHandlersEvent event
    ) {
        event.register(
                DamageDummyAttributesPayload.TYPE,
                DamageDummyAttributesClientPayloadHandler::handle
        );
    }

    public static void handle(
            DamageDummyAttributesPayload payload,
            IPayloadContext context
    ) {
        if (!(Minecraft.getInstance().screen
                instanceof DamageDummyScreen screen)) {
            return;
        }
        DamageDummyMenu menu = screen.getMenu();
        if (menu.containerId != payload.containerId()
                || payload.snapshot() == null
                || !menu.anchorPos().equals(payload.snapshot().anchorPos())) {
            return;
        }
        if (menu.replaceSnapshot(payload.snapshot())) {
            screen.refreshFromSnapshot();
        }
    }
}
