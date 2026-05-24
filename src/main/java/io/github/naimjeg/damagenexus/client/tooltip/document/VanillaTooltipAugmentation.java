package io.github.naimjeg.damagenexus.client.tooltip.document;

import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrative;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

public record VanillaTooltipAugmentation(
        Identifier enchantmentId,
        Component originalLineAnchor,
        RuleNarrative narrative,
        List<VanillaTooltipNote> detailNotes,
        List<String> applicationBuckets,
        List<Identifier> conditionTypeIds,
        List<Identifier> operationTypeIds
) implements TooltipSection {
    public VanillaTooltipAugmentation {
        Objects.requireNonNull(enchantmentId, "enchantmentId");
        Objects.requireNonNull(originalLineAnchor, "originalLineAnchor");
        Objects.requireNonNull(narrative, "narrative");
        detailNotes = detailNotes == null ? List.of() : List.copyOf(detailNotes);
        applicationBuckets = applicationBuckets == null ? List.of() : List.copyOf(applicationBuckets);
        conditionTypeIds = conditionTypeIds == null ? List.of() : List.copyOf(conditionTypeIds);
        operationTypeIds = operationTypeIds == null ? List.of() : List.copyOf(operationTypeIds);
    }
}
