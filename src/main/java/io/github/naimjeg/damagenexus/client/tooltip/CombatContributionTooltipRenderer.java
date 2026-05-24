package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.display.DamageContributionSummary;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CombatContributionTooltipRenderer {

    private CombatContributionTooltipRenderer() {
    }

    public static void renderSummaries(
            List<Component> tooltip,
            List<DamageContributionSummary> summaries,
            boolean detailMode
    ) {
        if (tooltip == null || summaries == null || summaries.isEmpty()) {
            return;
        }

        tooltip.add(
                Component.translatable("tooltip.damagenexus.contributions")
                        .withStyle(ChatFormatting.DARK_AQUA)
        );

        for (DamageContributionSummary summary : summaries) {
            if (summary == null) {
                continue;
            }

            tooltip.add(
                    Component.literal("  ")
                            .append(CombatContributionComponentFormatter.compact(summary))
                            .withStyle(ChatFormatting.DARK_GREEN)
            );

            if (detailMode) {
                renderDetails(tooltip, summary);
            }
        }
    }

    private static void renderDetails(
            List<Component> tooltip,
            DamageContributionSummary summary
    ) {
        tooltip.add(
                Component.literal("    status=" + summary.status()
                                + " source=" + summary.sourceKind()
                                + " op=" + summary.operationKind()
                                + " phase=" + summary.phase())
                        .withStyle(ChatFormatting.DARK_GRAY)
        );

        summary.displayGroup().ifPresent(group ->
                tooltip.add(
                        Component.literal("    display_group=" + group)
                                .withStyle(ChatFormatting.DARK_GRAY)
                )
        );

        summary.displaySubgroup().ifPresent(group ->
                tooltip.add(
                        Component.literal("    display_subgroup=" + group)
                                .withStyle(ChatFormatting.DARK_GRAY)
                )
        );

        summary.channel().ifPresent(channel ->
                tooltip.add(
                        Component.literal("    channel=" + channel)
                                .withStyle(ChatFormatting.DARK_GRAY)
                )
        );

        summary.applicationBucket().ifPresent(bucket ->
                tooltip.add(
                        Component.literal("    bucket=" + bucket)
                                .withStyle(ChatFormatting.DARK_GRAY)
                )
        );

        summary.preMultiplierBucket().ifPresent(bucket ->
                tooltip.add(
                        Component.literal("    pre_bucket=" + bucket)
                                .withStyle(ChatFormatting.DARK_GRAY)
                )
        );

        tooltip.add(
                Component.literal("    count=" + summary.count()
                                + " total=" + summary.totalValue())
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
