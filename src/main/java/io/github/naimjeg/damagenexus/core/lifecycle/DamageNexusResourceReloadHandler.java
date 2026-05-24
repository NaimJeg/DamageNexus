package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusPipeline;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.rule.DatapackDamageRuleReloadListener;
import io.github.naimjeg.damagenexus.core.template.DatapackDamageTemplateReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.List;

@EventBusSubscriber(modid = DamageNexus.MODID)
public final class DamageNexusResourceReloadHandler {

    private static final net.minecraft.resources.Identifier CHANNELS =
            DamageNexusIds.id("channel_registry");
    private static final net.minecraft.resources.Identifier GLOBAL_RULES =
            DamageNexusIds.id("global_damage_rules");
    private static final net.minecraft.resources.Identifier TEMPLATES =
            DamageNexusIds.id("static_damage_templates");
    private static final List<ReloadDependency> REQUIRED_DEPENDENCIES =
            List.of(
                    new ReloadDependency(CHANNELS, GLOBAL_RULES),
                    new ReloadDependency(CHANNELS, TEMPLATES)
            );

    private static final DamageNexusReloadAccess RELOAD_ACCESS =
            new DamageNexusReloadAccess();

    private DamageNexusResourceReloadHandler() {
    }

    @SubscribeEvent
    public static void onAddServerReloadListeners(
            AddServerReloadListenersEvent event
    ) {
        event.addListener(
                CHANNELS,
                new DamageChannelRegistry(RELOAD_ACCESS)
        );

        event.addListener(
                GLOBAL_RULES,
                new DatapackDamageRuleReloadListener(RELOAD_ACCESS)
        );

        event.addListener(
                TEMPLATES,
                new DatapackDamageTemplateReloadListener(RELOAD_ACCESS)
        );

        // SortedReloadListenerEvent graph edges: channel content is validated
        // before either dependent registry can publish its bound revision.
        for (ReloadDependency dependency : REQUIRED_DEPENDENCIES) {
            event.addDependency(dependency.first(), dependency.second());
        }

        DamageNexusPipeline.clearCache();
    }

    static List<ReloadDependency> requiredDependenciesForTesting() {
        return REQUIRED_DEPENDENCIES;
    }

    record ReloadDependency(
            net.minecraft.resources.Identifier first,
            net.minecraft.resources.Identifier second
    ) {}
}
