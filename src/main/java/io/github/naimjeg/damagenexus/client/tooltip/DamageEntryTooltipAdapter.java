package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class DamageEntryTooltipAdapter {

    private DamageEntryTooltipAdapter() {
    }

    public static List<DamageTooltipView> collectItemEntryViews(
            List<DamageEntryDefinition> entries
    ) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<DamageTooltipView> out = new ArrayList<>();

        for (DamageEntryDefinition entry : entries) {
            if (entry == null || !entry.display().hasVisibleText()) {
                continue;
            }

            out.add(toView(entry));
        }

        return List.copyOf(out);
    }

    public static void renderDebug(
            List<Component> tooltip,
            List<DamageEntryDefinition> entries,
            boolean sectionStarted
    ) {
        if (tooltip == null || entries == null || entries.isEmpty()) {
            return;
        }

        if (!sectionStarted) {
            tooltip.add(Component.literal("DamageNexus Debug")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.literal("Entries: " + entries.size())
                .withStyle(ChatFormatting.DARK_GRAY));

        for (DamageEntryDefinition entry : entries) {
            if (entry == null) {
                tooltip.add(Component.literal(" - <null entry>")
                        .withStyle(ChatFormatting.DARK_GRAY));
                continue;
            }

            tooltip.add(Component.literal(
                            " - " + entry.id()
                                    + " rules=" + entry.rules().size()
                                    + " stacking=" + entry.stacking()
                    )
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static DamageTooltipView toView(
            DamageEntryDefinition entry
    ) {
        DamageEntryDisplay display = entry.display();

        return new DamageTooltipView(
                entry.id(),
                component(display.name()),
                tooltipLines(entry),
                display.flavorText().map(DamageEntryTooltipAdapter::component),
                DamageAffixRarity.COMMON,
                entry.rules().stream().map(DamageRuleDefinition::id).toList(),
                "DAMAGE_ENTRY",
                display.showRuleBreakdown()
        );
    }

    private static List<Component> tooltipLines(
            DamageEntryDefinition entry
    ) {
        List<Component> lines = new ArrayList<>();

        for (DisplayText line : entry.display().tooltip()) {
            lines.add(component(line));
        }

        if (entry.display().showRuleBreakdown()) {
            for (DamageRuleDefinition rule : entry.rules()) {
                for (DamageRuleOperation operation : rule.operations()) {
                    lines.add(RuleTooltipDescriptions.describeOperation(
                            operation,
                            RuleTooltipMode.NORMAL
                    ));
                }
            }
        }

        return List.copyOf(lines);
    }

    private static Component component(DisplayText text) {
        if (text == null || text.isBlank()) {
            return Component.empty();
        }

        if (text.translate().isPresent()) {
            return Component.translatable(
                    text.translate().get(),
                    text.args().toArray()
            );
        }

        if (text.text().isPresent()) {
            return Component.literal(text.text().get());
        }

        return Component.literal(text.fallback().orElse(""));
    }
}
