package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRuleOrderingTest {

    @Test
    void damagePhaseDeclarationMatchesThePipelineExecutionOrder() {
        assertEquals(
                List.of(
                        DamagePhase.BASE_MODIFICATION,
                        DamagePhase.TYPE_SCALING,
                        DamagePhase.CRITICAL_HIT,
                        DamagePhase.CONDITIONAL_MULTI,
                        DamagePhase.GLOBAL_ADJUSTMENT,
                        DamagePhase.MITIGATION_SETUP,
                        DamagePhase.FINAL_OVERRIDE
                ),
                List.of(DamagePhase.values())
        );
    }

    @Test
    void comparatorUsesPhaseThenDescendingPriorityThenIdentifier() {
        DamageRuleDefinition baseLow = rule(
                "base_low", DamagePhase.BASE_MODIFICATION, 100
        );
        DamageRuleDefinition baseHigh = rule(
                "base_high", DamagePhase.BASE_MODIFICATION, 900
        );
        DamageRuleDefinition type400 = rule(
                "type_400", DamagePhase.TYPE_SCALING, 400
        );
        DamageRuleDefinition type401 = rule(
                "type_401", DamagePhase.TYPE_SCALING, 401
        );
        DamageRuleDefinition typeTieZ = rule(
                "type_tie_z", DamagePhase.TYPE_SCALING, 200
        );
        DamageRuleDefinition typeTieA = rule(
                "type_tie_a", DamagePhase.TYPE_SCALING, 200
        );

        assertEquals(
                List.of(
                        baseHigh,
                        baseLow,
                        type401,
                        type400,
                        typeTieA,
                        typeTieZ
                ),
                DamageRuleOrdering.sortedDefinitions(List.of(
                        type400,
                        typeTieZ,
                        baseLow,
                        type401,
                        baseHigh,
                        typeTieA
                ))
        );
    }

    @Test
    void emptyPresentationSequencesSortAfterExecutableSequences() {
        assertTrue(DamageRuleOrdering.compareDefinitionSequences(
                List.of(rule("base", DamagePhase.BASE_MODIFICATION, 0)),
                List.of()
        ) < 0);
    }

    private static DamageRuleDefinition rule(
            String path,
            DamagePhase phase,
            int priority
    ) {
        return new DamageRuleDefinition(
                Identifier.fromNamespaceAndPath("damagenexus_test", path),
                DamageRuleRole.OFFENSIVE,
                phase,
                priority,
                List.of(),
                List.of(),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }
}
