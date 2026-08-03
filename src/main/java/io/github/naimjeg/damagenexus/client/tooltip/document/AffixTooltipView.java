package io.github.naimjeg.damagenexus.client.tooltip.document;

import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record AffixTooltipView(
        Identifier id,
        Optional<DisplayText> name,
        List<DisplayText> authoredLines,
        Optional<DisplayText> flavorText,
        DamageAffixSlot slot,
        DamageAffixRarity rarity,
        DamageAffixStacking stacking,
        Optional<Identifier> stackingGroup,
        RuleBreakdownPolicy breakdownPolicy,
        List<EntryTooltipView> entries
) implements TooltipSection {
    public AffixTooltipView {
        name = name == null ? Optional.empty() : name;
        authoredLines = authoredLines == null ? List.of() : List.copyOf(authoredLines);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
        stackingGroup = stackingGroup == null ? Optional.empty() : stackingGroup;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
