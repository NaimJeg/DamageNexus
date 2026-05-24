package io.github.naimjeg.damagenexus.event.neoforge;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = DamageNexus.MODID)
public final class CaptureCleanupHandler {

    private CaptureCleanupHandler() {
    }

    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        clearThreadLocalCaptures();
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        clearThreadLocalCaptures();
    }

    private static void clearThreadLocalCaptures() {
        VanillaDamageCapture.clear();
        VanillaCritHandler.clear();
    }
}
