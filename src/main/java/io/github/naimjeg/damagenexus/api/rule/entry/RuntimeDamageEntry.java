package io.github.naimjeg.damagenexus.api.rule.entry;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleOwner;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.RuntimeDamageRule;
import net.minecraft.resources.Identifier;

import java.util.List;

public record RuntimeDamageEntry(
        DamageEntryDefinition definition,
        RuleExecutionContext executionContext
) {
    public Identifier id() {
        return definition.id();
    }

    public List<RuntimeDamageRule> expandRules() {
        if (!executionContext.matches(definition.slot())) {
            return List.of();
        }

        return definition.rules()
                .stream()
                .map(rule -> new RuntimeDamageRule(
                        rule,
                        executionContext,
                        DamageRuleOwner.entry(definition.id())
                ))
                .toList();
    }
}
