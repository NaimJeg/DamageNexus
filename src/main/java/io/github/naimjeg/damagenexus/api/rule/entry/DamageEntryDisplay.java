package io.github.naimjeg.damagenexus.api.rule.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;

import java.util.List;
import java.util.Optional;

public record DamageEntryDisplay(
        Optional<DisplayText> name,
        List<DisplayText> tooltip,
        Optional<DisplayText> flavorText,
        boolean showRuleBreakdown
) {
    public static final DamageEntryDisplay EMPTY =
            new DamageEntryDisplay(
                    Optional.empty(),
                    List.of(),
                    Optional.empty(),
                    false
            );

    public static final Codec<DamageEntryDisplay> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DisplayText.CODEC
                            .optionalFieldOf("name")
                            .forGetter(DamageEntryDisplay::name),

                    DamageRuleLimits.boundedList(
                                    DisplayText.CODEC,
                                    DamageRuleLimits.MAX_TOOLTIP_LINES,
                                    "entry tooltip"
                            )
                            .optionalFieldOf("tooltip", List.of())
                            .forGetter(DamageEntryDisplay::tooltip),

                    DisplayText.CODEC
                            .optionalFieldOf("flavor_text")
                            .forGetter(DamageEntryDisplay::flavorText),

                    Codec.BOOL
                            .optionalFieldOf("show_rule_breakdown", false)
                            .forGetter(DamageEntryDisplay::showRuleBreakdown)
            ).apply(instance, DamageEntryDisplay::new));

    public DamageEntryDisplay {
        name = name == null ? Optional.empty() : name;
        tooltip = tooltip == null ? List.of() : List.copyOf(tooltip);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
    }

    public boolean hasVisibleText() {
        return name.isPresent()
                || !tooltip.isEmpty()
                || flavorText.isPresent()
                || showRuleBreakdown;
    }

    public DamageEntryDisplay(
            DisplayText name,
            List<DisplayText> tooltip,
            Optional<DisplayText> flavorText,
            boolean showRuleBreakdown
    ) {
        this(Optional.ofNullable(name), tooltip, flavorText, showRuleBreakdown);
    }
}
