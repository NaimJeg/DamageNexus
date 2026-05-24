package io.github.naimjeg.damagenexus.api.rule.affix;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleOwner;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.RuntimeDamageRule;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import net.minecraft.resources.Identifier;

import java.util.List;

public record RuntimeDamageAffix(
        DamageAffixDefinition definition,
        RuleExecutionContext executionContext
) {
    public Identifier id() {
        return definition.id();
    }

    public List<RuntimeDamageRule> expandRules() {
        if (!executionContext.matches(definition.slot())) {
            return List.of();
        }

        return definition.entries()
                .stream()
                .filter(entry -> executionContext.matches(entry.slot()))
                .flatMap(entry -> expandEntry(entry).stream())
                .toList();
    }

    private List<RuntimeDamageRule> expandEntry(
            DamageEntryDefinition entry
    ) {
        return entry.rules()
                .stream()
                .map(rule -> new RuntimeDamageRule(
                        rule,
                        executionContext,
                        DamageRuleOwner.affixEntry(
                                definition.id(),
                                entry.id()
                        )
                ))
                .toList();
    }
}
