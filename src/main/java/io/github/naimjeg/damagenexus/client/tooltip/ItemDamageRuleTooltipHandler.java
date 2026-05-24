package io.github.naimjeg.damagenexus.client.tooltip;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSelectionResolver;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySelectionResolver;
import io.github.naimjeg.damagenexus.client.tooltip.document.DamageTooltipDocument;
import io.github.naimjeg.damagenexus.client.tooltip.document.DamageTooltipDocumentPlanner;
import io.github.naimjeg.damagenexus.client.tooltip.document.VanillaTooltipAugmentation;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrativePlanner;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.config.TooltipDebugLevel;
import io.github.naimjeg.damagenexus.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@EventBusSubscriber(modid = DamageNexus.MODID, value = Dist.CLIENT)
final class ItemDamageRuleTooltipHandler {
    private ItemDamageRuleTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var registry = DamageNexusClientTooltips.registry();
        RuleNarrativePlanner narratives = new RuleNarrativePlanner(registry);
        ItemStack stack = event.getItemStack();

        List<DamageEntryDefinition> entries = stack.getOrDefault(
                ModDataComponents.DAMAGE_ENTRIES.get(), List.of()
        );
        List<DamageAffixDefinition> affixes = stack.getOrDefault(
                ModDataComponents.DAMAGE_AFFIXES.get(), List.of()
        );
        DamageItemTemplateReferences references = stack.getOrDefault(
                ModDataComponents.DAMAGE_TEMPLATE_REFERENCES.get(),
                DamageItemTemplateReferences.EMPTY
        );

        List<DamageEntryDefinition> selectedEntries =
                DamageEntrySelectionResolver.resolve(entries);
        List<DamageAffixDefinition> selectedAffixes =
                DamageAffixSelectionResolver.resolve(affixes);
        List<VanillaTooltipAugmentation> vanilla =
                VanillaEnchantmentTooltipAdapter.collect(stack, narratives);
        TooltipDebugLevel debugLevel =
                DamageNexusConfig.current().tooltips().debugLevel();

        DamageTooltipDocument document = new DamageTooltipDocumentPlanner(narratives)
                .plan(selectedEntries, selectedAffixes, vanilla, references, debugLevel);
        if (document.isEmpty()) {
            return;
        }

        TooltipDetailLevel detailLevel = event.getFlags().hasShiftDown() || isShiftDown()
                ? TooltipDetailLevel.EXPANDED
                : TooltipDetailLevel.COMPACT;
        DamageTooltipRenderer renderer = new DamageTooltipRenderer(
                narratives,
                new RulePhraseRenderer(registry)
        );
        renderer.render(
                event.getToolTip(),
                document,
                new TooltipPresentationPolicy(detailLevel, debugLevel)
        );
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
