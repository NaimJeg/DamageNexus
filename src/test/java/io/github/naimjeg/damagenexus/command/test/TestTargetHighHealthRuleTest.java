package io.github.naimjeg.damagenexus.command.test;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.TargetHealthAboveCondition;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddGlobalPostMultiplierOperation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestTargetHighHealthRuleTest {

    @Test
    void ruleReusesExistingConditionalPhaseAndPost25Operation() {
        DamageRuleDefinition rule =
                TestRuleFactory.targetHighHealthGlobalPost25();

        assertEquals(DamagePhase.CONDITIONAL_MULTI, rule.phase());
        assertEquals(1, rule.conditions().size());
        assertEquals(1, rule.operations().size());

        assertTrue(rule.conditions().stream().anyMatch(
                condition -> condition
                        instanceof TargetHealthAboveCondition threshold
                        && threshold.threshold() == 0.80f
        ));

        assertTrue(rule.operations().stream().anyMatch(
                operation -> operation
                        instanceof AddGlobalPostMultiplierOperation multiplier
                        && multiplier.value() == 0.25f
        ));
    }
}
