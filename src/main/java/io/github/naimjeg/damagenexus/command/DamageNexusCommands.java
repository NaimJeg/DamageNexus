package io.github.naimjeg.damagenexus.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.config.DiagnosticMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = DamageNexus.MODID)
public final class DamageNexusCommands {

    private DamageNexusCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var config = DamageNexusConfig.current();

        boolean diagnosticsEnabled = config.diagnostics().diagnosticMode()
                != DiagnosticMode.OFF;
        boolean testCommands = config.developer().testCommandsEnabled();

        if (!diagnosticsEnabled && !testCommands) {
            return;
        }

        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var root = Commands.literal("damagenexus");

        if (testCommands) {
            DamageTestCommands.register(root);
            DamageItemCommands.register(root);
            DamageDamageCommands.register(root);
            DamageBypassCommands.register(root);
            DamageMobCommands.register(root);
        }

        if (diagnosticsEnabled) {
            DamageEffectCommands.register(root);
            DamageAttributeCommands.register(root);
        }

        if (testCommands || diagnosticsEnabled) {
            DamageCleanupCommands.register(root);
        }

        DamageCommandSecurity.verifyTree(root.build());
        dispatcher.register(root);
    }
}
