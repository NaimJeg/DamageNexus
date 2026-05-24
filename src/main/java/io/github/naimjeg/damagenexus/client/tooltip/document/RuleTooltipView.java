package io.github.naimjeg.damagenexus.client.tooltip.document;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrative;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RuleTooltipView(
        Identifier id,
        RuleNarrative narrative,
        DamagePhase phase,
        DamageRuleRole role,
        int priority,
        DamageRuleStacking stacking,
        Optional<Identifier> stackingGroup,
        Optional<String> traceLabel,
        List<Identifier> conditionTypeIds,
        List<Identifier> operationTypeIds
) {
    public RuleTooltipView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(narrative, "narrative");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(stacking, "stacking");
        stackingGroup = stackingGroup == null ? Optional.empty() : stackingGroup;
        traceLabel = traceLabel == null ? Optional.empty() : traceLabel;
        conditionTypeIds = conditionTypeIds == null ? List.of() : List.copyOf(conditionTypeIds);
        operationTypeIds = operationTypeIds == null ? List.of() : List.copyOf(operationTypeIds);
    }
}
