package io.github.naimjeg.damagenexus.api.rule.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;

import java.util.List;
import java.util.Optional;

public record DamageAffixDisplay(
        DisplayText name,
        List<DisplayText> tooltip,
        Optional<DisplayText> flavorText,
        boolean showRuleBreakdown
) {
    public static final DamageAffixDisplay EMPTY =
            new DamageAffixDisplay(
                    DisplayText.EMPTY,
                    List.of(),
                    Optional.empty(),
                    false
            );

    public static final Codec<DamageAffixDisplay> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DisplayText.CODEC
                            .fieldOf("name")
                            .forGetter(DamageAffixDisplay::name),

                    DamageRuleLimits.boundedList(
                                    DisplayText.CODEC,
                                    DamageRuleLimits.MAX_TOOLTIP_LINES,
                                    "affix tooltip"
                            )
                            .optionalFieldOf("tooltip", List.of())
                            .forGetter(DamageAffixDisplay::tooltip),

                    DisplayText.CODEC
                            .optionalFieldOf("flavor_text")
                            .forGetter(DamageAffixDisplay::flavorText),

                    Codec.BOOL
                            .optionalFieldOf("show_rule_breakdown", false)
                            .forGetter(DamageAffixDisplay::showRuleBreakdown)
            ).apply(instance, DamageAffixDisplay::new));

    public DamageAffixDisplay {
        name = name == null ? DisplayText.EMPTY : name;
        tooltip = tooltip == null ? List.of() : List.copyOf(tooltip);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
    }

    public boolean hasVisibleText() {
        return !name.isBlank()
                || !tooltip.isEmpty()
                || flavorText.filter(text -> !text.isBlank()).isPresent();
    }
}
