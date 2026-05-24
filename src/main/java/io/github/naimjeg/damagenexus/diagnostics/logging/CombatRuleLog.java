package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.core.rule.StackingTrace;
import io.github.naimjeg.damagenexus.core.trace.RuleSkipReason;

public interface CombatRuleLog {

    void collected(
            DamagePhase phase,
            DamageRuleDefinition rule,
            RuleExecutionContext exec
    );

    void skipped(
            DamagePhase phase,
            DamageRuleDefinition rule,
            RuleSkipReason reason
    );

    void phaseMismatch(
            DamagePhase runningPhase,
            DamageRuleDefinition rule
    );

    void roleMismatch(
            DamagePhase phase,
            DamageRuleDefinition rule,
            RuleExecutionContext exec
    );

    void conditionFailed(
            DamagePhase phase,
            DamageRuleDefinition rule,
            DamageRuleCondition condition
    );

    void executed(
            DamagePhase phase,
            DamageRuleDefinition rule
    );

    void result(
            DamagePhase phase,
            DamageRuleDefinition rule,
            int appliedOperations,
            int rejectedOperations,
            int noOpOperations
    );

    void stackingDrop(StackingTrace trace);
}

