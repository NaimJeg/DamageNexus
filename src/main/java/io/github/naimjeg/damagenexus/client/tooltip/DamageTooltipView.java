package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record DamageTooltipView(
        Identifier id,
        Component displayName,
        List<Component> tooltipLines,
        Optional<Component> flavorText,
        DamageAffixRarity rarity,
        List<Identifier> ruleIds,
        String debugSource,
        boolean showRuleBreakdown
) {
    public DamageTooltipView {
        tooltipLines = tooltipLines == null
                ? List.of()
                : List.copyOf(tooltipLines);

        flavorText = flavorText == null
                ? Optional.empty()
                : flavorText;

        ruleIds = ruleIds == null
                ? List.of()
                : List.copyOf(ruleIds);

        rarity = rarity == null
                ? DamageAffixRarity.COMMON
                : rarity;

        debugSource = debugSource == null ? "" : debugSource;
    }

    public String debugDisplayName() {
        return displayName == null ? "" : displayName.getString();
    }
}
