package io.github.naimjeg.damagenexus.client.screen;

import io.github.naimjeg.damagenexus.registry.ModMenuTypes;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Physical-client-only registration of container screens via
 * {@link RegisterMenuScreensEvent}. Referenced exclusively from
 * {@code ModClientHandler} (the {@code Dist.CLIENT} mod entry point), so this
 * class and {@link DamageDummyScreen} are never loaded on a dedicated
 * server.
 */
public final class ModMenuScreens {

    private ModMenuScreens() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                ModMenuTypes.DAMAGE_DUMMY.get(),
                DamageDummyScreen::new
        );
    }
}
