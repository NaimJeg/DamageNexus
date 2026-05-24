package io.github.naimjeg.damagenexus;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.network.DamageNexusNetwork;
import io.github.naimjeg.damagenexus.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(DamageNexus.MODID)
public class DamageNexus {
    public static final String MODID = "damagenexus";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public DamageNexus(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(DamageNexusConfig::onLoad);
        modEventBus.addListener(DamageNexusConfig::onReload);
        modEventBus.addListener(DamageNexusNetwork::registerPayloads);


        ModAttributes.register(modEventBus);
        ModDamageProcessors.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // Creative inventory integration: append the pedestal BlockItem to
        // the vanilla Functional Blocks tab. BuildCreativeModeTabContentsEvent
        // is a mod-bus event, so it is registered here rather than on the
        // game (NeoForge.EVENT_BUS) or inside the physical-client handler.
        modEventBus.addListener(ModCreativeTabContents::buildContents);

        // Damage dummy attribute lifecycle: a baseline supplier first, then
        // generic attachment of the full registered ATTRIBUTE registry.
        modEventBus.addListener(ModEntityAttributes::onCreateAttributes);
        modEventBus.addListener(ModEntityAttributes::onModifyAttributes);

        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON,
                DamageNexusConfig.SPEC
        );
    }

}

