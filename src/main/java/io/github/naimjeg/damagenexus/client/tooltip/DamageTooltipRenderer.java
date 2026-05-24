package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class DamageTooltipRenderer {

    private DamageTooltipRenderer() {
    }

    public static void renderTooltipViews(
            List<Component> tooltip,
            List<DamageTooltipView> views,
            boolean detailMode
    ) {
        if (tooltip == null || views == null || views.isEmpty()) {
            return;
        }

        for (DamageTooltipView view : views) {
            if (view == null) {
                continue;
            }

            appendTooltipView(tooltip, view, detailMode);
        }
    }

    private static void appendTooltipView(
            List<Component> tooltip,
            DamageTooltipView view,
            boolean detailMode
    ) {
        if (view.displayName() != null
                && !view.displayName().getString().isBlank()) {
            tooltip.add(
                    view.displayName()
                            .copy()
                            .withStyle(styleForRarity(view.rarity().name()))
            );
        }

        for (Component line : view.tooltipLines()) {
            if (line == null || line.getString().isBlank()) {
                continue;
            }

            tooltip.add(
                    Component.literal("  ")
                            .append(line.copy())
                            .withStyle(ChatFormatting.DARK_GREEN)
            );
        }

        if (detailMode) {
            view.flavorText()
                    .filter(text -> !text.getString().isBlank())
                    .ifPresent(flavor -> tooltip.add(
                            Component.literal("  ")
                                    .append(flavor.copy())
                                    .withStyle(
                                            ChatFormatting.DARK_GRAY,
                                            ChatFormatting.ITALIC
                                    )
                    ));
        }

        if (view.showRuleBreakdown() && detailMode) {
            tooltip.add(
                    Component.translatable("tooltip.damagenexus.rules")
                            .withStyle(ChatFormatting.DARK_AQUA)
            );

            for (Identifier ruleId : view.ruleIds()) {
                tooltip.add(
                        Component.literal("    " + ruleId)
                                .withStyle(ChatFormatting.DARK_AQUA)
                );
            }
        }
    }

    public static void renderItemAffixes(
            List<Component> tooltip,
            List<DamageAffixDefinition> affixes,
            boolean detailMode
    ) {
        renderTooltipViews(
                tooltip,
                collectItemAffixViews(affixes),
                detailMode
        );
    }

    public static List<DamageTooltipView> collectItemAffixViews(
            List<DamageAffixDefinition> affixes
    ) {
        if (affixes == null || affixes.isEmpty()) {
            return List.of();
        }

        return affixes.stream()
                .map(DamageTooltipRenderer::toTooltipView)
                .toList();
    }

    private static DamageTooltipView toTooltipView(
            DamageAffixDefinition affix
    ) {
        DamageAffixDisplay display = affix.display();

        return new DamageTooltipView(
                affix.id(),
                DisplayTextResolver.resolve(display.name()),
                display.tooltip()
                        .stream()
                        .map(DisplayTextResolver::resolve)
                        .toList(),
                display.flavorText().map(DisplayTextResolver::resolve),
                affix.rarity(),
                affix.entries()
                        .stream()
                        .map(DamageEntryDefinition::id)
                        .toList(),
                "",
                display.showRuleBreakdown()
        );
    }

    public static boolean renderDebug(
            List<Component> tooltip,
            List<DamageAffixDefinition> affixes,
            boolean sectionAlreadyStarted
    ) {
        if (tooltip == null || affixes == null || affixes.isEmpty()) {
            return sectionAlreadyStarted;
        }

        if (!sectionAlreadyStarted) {
            tooltip.add(
                    Component.translatable("tooltip.damagenexus.debug.header")
                            .withStyle(ChatFormatting.DARK_AQUA)
            );
        }

        for (DamageAffixDefinition affix : affixes) {
            tooltip.add(
                    Component.literal("  Affix: ")
                            .append(Component.literal(affix.id().toString()))
                            .withStyle(ChatFormatting.DARK_AQUA)
            );

            tooltip.add(debugLine("rarity", affix.rarity().name()));
            tooltip.add(debugLine("slot", affix.slot().name()));
            tooltip.add(debugLine("stacking", affix.stacking().name()));

            affix.stackingGroup()
                    .ifPresent(group -> tooltip.add(debugLine(
                            "stacking_group",
                            group.toString()
                    )));

            for (DamageEntryDefinition entry : affix.entries()) {
                tooltip.add(
                        Component.literal("    entry=")
                                .append(Component.literal(entry.id().toString()))
                                .withStyle(ChatFormatting.DARK_AQUA)
                );

                tooltip.add(debugLine("    entry_stacking", entry.stacking().name()));

                for (DamageRuleDefinition rule : entry.rules()) {
                    tooltip.add(
                            Component.literal("      rule=")
                                    .append(Component.literal(rule.id().toString()))
                                    .withStyle(ChatFormatting.DARK_AQUA)
                    );

                    tooltip.add(debugLine("      phase", rule.phase().name()));
                    tooltip.add(debugLine("      role", rule.role().name()));
                    tooltip.add(debugLine("      priority", Integer.toString(rule.priority())));
                }
            }
        }

        return true;
    }

    public static boolean renderTooltipViewDebug(
            List<Component> tooltip,
            List<DamageTooltipView> views,
            boolean sectionAlreadyStarted
    ) {
        if (tooltip == null || views == null || views.isEmpty()) {
            return sectionAlreadyStarted;
        }

        for (DamageTooltipView view : views) {
            if (view == null) {
                continue;
            }

            tooltip.add(
                    Component.literal("  ")
                            .append(Component.literal(view.id().toString()))
                            .withStyle(ChatFormatting.DARK_AQUA)
            );

            tooltip.add(tooltipViewDebugLine("source", view.id().toString()));
            tooltip.add(tooltipViewDebugLine(
                    "source",
                    safeDebugText(view.debugSource())
            ));
            tooltip.add(tooltipViewDebugLine("rarity", view.rarity().name()));

            String displayName = view.debugDisplayName();
            if (!displayName.isBlank()) {
                tooltip.add(tooltipViewDebugLine("display", displayName));
            }

            if (view.ruleIds().isEmpty()) {
                tooltip.add(tooltipViewDebugLine("rules", "<none>"));
            } else {
                for (Identifier ruleId : view.ruleIds()) {
                    tooltip.add(tooltipViewDebugLine("rule", ruleId.toString()));
                }
            }
        }

        return true;
    }

    private static Component tooltipViewDebugLine(
            String key,
            String value
    ) {
        return Component.literal("    " + key + ": " + value)
                .withStyle(ChatFormatting.DARK_AQUA);
    }

    private static Component debugLine(
            String key,
            String value
    ) {
        return Component.literal("    " + key + "=" + value)
                .withStyle(ChatFormatting.DARK_AQUA);
    }

    private static String safeDebugText(String value) {
        return value == null || value.isBlank()
                ? "<none>"
                : value;
    }

    private static ChatFormatting styleForRarity(String rarity) {
        return switch (rarity) {
            case "UNCOMMON" -> ChatFormatting.GREEN;
            case "RARE" -> ChatFormatting.BLUE;
            case "EPIC" -> ChatFormatting.DARK_PURPLE;
            case "LEGENDARY" -> ChatFormatting.GOLD;
            case "UNIQUE" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GRAY;
        };
    }
}
