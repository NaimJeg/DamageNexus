package io.github.naimjeg.damagenexus.client;

import io.github.naimjeg.damagenexus.DamageNexus;
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
    }

}
