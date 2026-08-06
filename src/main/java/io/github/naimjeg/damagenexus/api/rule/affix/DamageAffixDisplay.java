package io.github.naimjeg.damagenexus.api.rule.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;

import java.util.List;
import java.util.Optional;

public record DamageAffixDisplay(
        Optional<DisplayText> name,
        List<DisplayText> authoredSummary,
        Optional<DisplayText> flavorText,
        boolean showRuleBreakdown
) {
    public static final DamageAffixDisplay EMPTY =
            new DamageAffixDisplay(
                    Optional.empty(),
                    List.of(),
                    Optional.empty(),
                    false
            );

    private record SerializedDisplay(
            Optional<DisplayText> name,
            Optional<List<DisplayText>> authoredSummary,
            Optional<List<DisplayText>> legacyTooltip,
            Optional<DisplayText> flavorText,
            boolean showRuleBreakdown
    ) {
        public SerializedDisplay {
            name = name == null ? Optional.empty() : name;
            authoredSummary = authoredSummary == null ? Optional.empty() : authoredSummary;
            legacyTooltip = legacyTooltip == null ? Optional.empty() : legacyTooltip;
            flavorText = flavorText == null ? Optional.empty() : flavorText;
        }
    }

    private static final Codec<SerializedDisplay> WIRE_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    DisplayText.CODEC
                            .optionalFieldOf("name")
                            .forGetter(SerializedDisplay::name),

                    DamageRuleLimits.boundedList(
                                    DisplayText.CODEC,
                                    DamageRuleLimits.MAX_TOOLTIP_LINES,
                                    "affix authored summary"
                            )
                            .optionalFieldOf("authored_summary")
                            .forGetter(SerializedDisplay::authoredSummary),

                    DamageRuleLimits.boundedList(
                                    DisplayText.CODEC,
                                    DamageRuleLimits.MAX_TOOLTIP_LINES,
                                    "affix legacy tooltip"
                            )
                            .optionalFieldOf("tooltip")
                            .forGetter(SerializedDisplay::legacyTooltip),

                    DisplayText.CODEC
                            .optionalFieldOf("flavor_text")
                            .forGetter(SerializedDisplay::flavorText),

                    Codec.BOOL
                            .optionalFieldOf("show_rule_breakdown", false)
                            .forGetter(SerializedDisplay::showRuleBreakdown)
            ).apply(instance, SerializedDisplay::new));

    public static final Codec<DamageAffixDisplay> CODEC = WIRE_CODEC.flatXmap(
            DamageAffixDisplay::decode,
            display -> DataResult.success(new SerializedDisplay(
                    display.name(),
                    Optional.of(display.authoredSummary()),
                    Optional.empty(),
                    display.flavorText(),
                    display.showRuleBreakdown()
            ))
    );

    private static DataResult<DamageAffixDisplay> decode(SerializedDisplay serialized) {
        Optional<List<DisplayText>> authoredSummary = serialized.authoredSummary();
        Optional<List<DisplayText>> legacyTooltip = serialized.legacyTooltip();
        if (authoredSummary.isPresent()
                && legacyTooltip.isPresent()
                && !authoredSummary.get().equals(legacyTooltip.get())) {
            return DataResult.error(() ->
                    "Damage affix display has conflicting authored_summary and tooltip fields"
            );
        }
        return DataResult.success(new DamageAffixDisplay(
                serialized.name(),
                authoredSummary.or(() -> legacyTooltip).orElse(List.of()),
                serialized.flavorText(),
                serialized.showRuleBreakdown()
        ));
    }

    public DamageAffixDisplay {
        name = name == null ? Optional.empty() : name;
        authoredSummary = authoredSummary == null ? List.of() : List.copyOf(authoredSummary);
        flavorText = flavorText == null ? Optional.empty() : flavorText;
    }

    public boolean hasVisibleText() {
        return name.isPresent()
                || !authoredSummary.isEmpty()
                || flavorText.isPresent()
                || showRuleBreakdown;
    }

    /**
     * @deprecated Use {@link #authoredSummary()}.
     */
    @Deprecated(forRemoval = false)
    public List<DisplayText> tooltip() {
        return authoredSummary;
    }

    public DamageAffixDisplay(
            DisplayText name,
            List<DisplayText> authoredSummary,
            Optional<DisplayText> flavorText,
            boolean showRuleBreakdown
    ) {
        this(Optional.ofNullable(name), authoredSummary, flavorText, showRuleBreakdown);
    }
}
