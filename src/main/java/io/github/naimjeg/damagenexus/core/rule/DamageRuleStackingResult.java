package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.rule.RuntimeDamageRule;

import java.util.List;

public record DamageRuleStackingResult(
        List<RuntimeDamageRule> rules,
        List<StackingTrace> traces
) {
}
