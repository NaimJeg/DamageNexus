package io.github.naimjeg.damagenexus.api.rule;

public record RuntimeDamageRule(
        DamageRuleDefinition definition,
        RuleExecutionContext executionContext,
        DamageRuleOwner owner
) {
    public RuntimeDamageRule(
            DamageRuleDefinition definition,
            RuleExecutionContext executionContext
    ) {
        this(
                definition,
                executionContext,
                DamageRuleOwner.rule()
        );
    }

    public RuntimeDamageRule {
        owner = owner == null ? DamageRuleOwner.rule() : owner;
    }
}