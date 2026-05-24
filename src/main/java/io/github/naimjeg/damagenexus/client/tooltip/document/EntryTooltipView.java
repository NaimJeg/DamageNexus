package io.github.naimjeg.damagenexus.client.tooltip.document;

import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record EntryTooltipView(
        Identifier id,
        Optional<DisplayText> name,
        List<DisplayText> authoredSummary,
        Optional<DisplayText> flavorText,
        DamageEntrySlot slot,
        DamageEntryStacking stacking,
        Optional<Identifier> stackingGroup,
        RuleBreakdownPolicy breakdownPolicy,
        List<RuleTooltipView> rules
) implements TooltipSection {
    public EntryTooltipView {
        name = name == null ? Optional.empty() : name;
        authoredSummary = authoredSummary == null
                ? List.of() : List.copyOf(authoredSummary);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
        stackingGroup = stackingGroup == null ? Optional.empty() : stackingGroup;
        rules = rules == null ? List.of() : List.copyOf(rules);
    }
}
