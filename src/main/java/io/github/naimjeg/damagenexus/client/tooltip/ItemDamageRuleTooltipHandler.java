package io.github.naimjeg.damagenexus.client.tooltip;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSelectionResolver;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySelectionResolver;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(
        modid = DamageNexus.MODID,
        value = Dist.CLIENT
)
final class ItemDamageRuleTooltipHandler {

    private ItemDamageRuleTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        DamageNexusClientTooltips.register();

        ItemStack stack = event.getItemStack();

        List<DamageEntryDefinition> entries =
                stack.getOrDefault(
                        ModDataComponents.DAMAGE_ENTRIES.get(),
                        List.of()
                );

        List<DamageAffixDefinition> affixes =
                stack.getOrDefault(
                        ModDataComponents.DAMAGE_AFFIXES.get(),
                        List.of()
                );
        DamageItemTemplateReferences templateReferences =
                stack.getOrDefault(
                        ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get(),
                        DamageItemTemplateReferences.EMPTY
                );

        List<DamageEntryDefinition> selectedEntries =
                DamageEntrySelectionResolver.resolve(entries);

        List<DamageAffixDefinition> selectedAffixes =
                DamageAffixSelectionResolver.resolve(affixes);

        List<DamageTooltipView> vanillaEnchantmentViews =
                VanillaEnchantmentTooltipAdapter.collectTooltipViews(stack);

        List<DamageTooltipView> tooltipViews = new ArrayList<>();
        tooltipViews.addAll(DamageEntryTooltipAdapter.collectItemEntryViews(
                selectedEntries
        ));
        tooltipViews.addAll(DamageTooltipRenderer.collectItemAffixViews(
                selectedAffixes
        ));
        tooltipViews.addAll(vanillaEnchantmentViews);

        if (tooltipViews.isEmpty() && templateReferences.isEmpty()) {
            return;
        }

        boolean detailMode = event.getFlags().hasShiftDown() || isShiftDown();
        boolean debugTooltipsEnabled =
                DamageNexusConfig.current().tooltips().debugTooltipsEnabled();

        List<Component> tooltip = event.getToolTip();

        DamageTooltipRenderer.renderTooltipViews(
                tooltip,
                tooltipViews,
                detailMode
        );

        renderTemplateReferences(tooltip, templateReferences);

        if (debugTooltipsEnabled) {
            boolean debugSectionStarted = DamageTooltipRenderer.renderDebug(
                    tooltip,
                    selectedAffixes,
                    false
            );

            DamageEntryTooltipAdapter.renderDebug(
                    tooltip,
                    selectedEntries,
                    debugSectionStarted
            );

            DamageTooltipRenderer.renderTooltipViewDebug(
                    tooltip,
                    vanillaEnchantmentViews,
                    debugSectionStarted
            );
        }
    }

    private static void renderTemplateReferences(
            List<Component> tooltip,
            DamageItemTemplateReferences references
    ) {
        int shown = 0;
        for (var reference : references.entries()) {
            if (shown++ >= 8) break;
            tooltip.add(Component.translatable(
                    "tooltip.damagenexus.template.entry_reference",
                    limitedId(reference.id().toString())));
        }
        shown = 0;
        for (var reference : references.affixes()) {
            if (shown++ >= 8) break;
            tooltip.add(Component.translatable(
                    "tooltip.damagenexus.template.affix_reference",
                    limitedId(reference.id().toString())));
        }
    }

    private static String limitedId(String value) {
        String sanitized = value.replace('\n', '_').replace('\r', '_');
        return sanitized.length() <= 128
                ? sanitized
                : sanitized.substring(0, 128);
    }

    private static boolean isShiftDown() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        Window window = minecraft.getWindow();

        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
