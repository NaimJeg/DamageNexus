package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.builtin.rule.condition.*;
import io.github.naimjeg.damagenexus.builtin.rule.operation.AddBaseDamageOperation;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.ConditionExpression;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrative;
import io.github.naimjeg.damagenexus.client.tooltip.narrative.RuleNarrativePlanner;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuleNarrativePlannerTest {
    @Test
    void allOfAnyOfAndNotKeepTheirNestedStructure() {
        RuleNarrative narrative = planner().plan(rule(List.of(
                new AllOfCondition(List.of(
                        new TargetOnFireCondition(),
                        new AnyOfCondition(List.of(
                                new IsCriticalCondition(),
                                new NotCondition(new ProcAllowedCondition())
                        ))
                ))
        )));

        ConditionExpression.AllOf implicit = assertInstanceOf(
                ConditionExpression.AllOf.class, narrative.condition()
        );
        assertTrue(implicit.implicit());
        ConditionExpression.AllOf explicit = assertInstanceOf(
                ConditionExpression.AllOf.class, implicit.children().getFirst()
        );
        assertFalse(explicit.implicit());
        assertInstanceOf(ConditionExpression.Phrase.class, explicit.children().get(0));
        ConditionExpression.AnyOf any = assertInstanceOf(
                ConditionExpression.AnyOf.class, explicit.children().get(1)
        );
        assertInstanceOf(ConditionExpression.Not.class, any.children().get(1));
    }

    @Test
    void definitionConditionListIsAnExplicitlyMarkedImplicitAllOf() {
        RuleNarrative narrative = planner().plan(rule(List.of(
                new TargetOnFireCondition(),
                new IsCriticalCondition()
        )));
        ConditionExpression.AllOf root = assertInstanceOf(
                ConditionExpression.AllOf.class, narrative.condition()
        );
        assertTrue(root.implicit());
        assertEquals(2, root.children().size());
        assertEquals(1, narrative.effects().size());
    }

    private static RuleNarrativePlanner planner() {
        var registry = new io.github.naimjeg.damagenexus.api.client.phrase.RulePhraseRegistry();
        DamageNexusRulePhraseBootstrap.register(registry);
        registry.freeze();
        return new RuleNarrativePlanner(registry);
    }

    private static DamageRuleDefinition rule(
            List<io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition> conditions
    ) {
        return new DamageRuleDefinition(
                id("rule"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                conditions,
                List.of(new AddBaseDamageOperation(id("fire"), 4.0f)),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("damagenexus_test", path);
    }
}
