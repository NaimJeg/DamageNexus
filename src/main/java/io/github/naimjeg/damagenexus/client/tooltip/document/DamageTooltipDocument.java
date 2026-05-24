package io.github.naimjeg.damagenexus.client.tooltip.document;

import java.util.List;
import java.util.Optional;

public record DamageTooltipDocument(
        List<AffixTooltipView> affixes,
        List<EntryTooltipView> standaloneEntries,
        List<VanillaTooltipAugmentation> vanillaAugmentations,
        List<TemplateReferenceTooltipView> templateReferences,
        Optional<DebugTooltipSection> debugSection
) {
    public DamageTooltipDocument {
        affixes = affixes == null ? List.of() : List.copyOf(affixes);
        standaloneEntries = standaloneEntries == null ? List.of() : List.copyOf(standaloneEntries);
        vanillaAugmentations = vanillaAugmentations == null ? List.of() : List.copyOf(vanillaAugmentations);
        templateReferences = templateReferences == null ? List.of() : List.copyOf(templateReferences);
        debugSection = debugSection == null ? Optional.empty() : debugSection;
    }

    public boolean isEmpty() {
        return affixes.isEmpty()
                && standaloneEntries.isEmpty()
                && vanillaAugmentations.isEmpty()
                && templateReferences.isEmpty();
    }
}
