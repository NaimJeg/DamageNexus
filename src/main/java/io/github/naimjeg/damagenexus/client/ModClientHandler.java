package io.github.naimjeg.damagenexus.client;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.client.damage.DamageNumberClientPayloadHandler;
import io.github.naimjeg.damagenexus.client.screen.ModMenuScreens;
import io.github.naimjeg.damagenexus.client.screen.DamageDummyAttributesClientPayloadHandler;
import io.github.naimjeg.damagenexus.client.tooltip.DamageNexusClientTooltips;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = DamageNexus.MODID, dist = Dist.CLIENT)
public class ModClientHandler {

    public ModClientHandler(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                ConfigurationScreen::new
        );

        DamageNexusClientTooltips.register();
        container.getEventBus().addListener(
                DamageNumberClientPayloadHandler
                        ::onRegisterPayloadHandlers
        );
        container.getEventBus().addListener(
                DamageDummyAttributesClientPayloadHandler
                        ::onRegisterPayloadHandlers
        );
        container.getEventBus().addListener(
                DamageDummyModel::registerLayerDefinitions
        );
        container.getEventBus().addListener(
                DamageDummyRenderer::registerRenderers
        );
        container.getEventBus().addListener(
                ModMenuScreens::registerScreens
        );
    }

}
