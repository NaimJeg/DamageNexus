package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.api.damage.DamageLineage;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.damage.DamageTriggerPolicy;
import io.github.naimjeg.damagenexus.config.DamageSafetySettings;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageAdmissionControllerTest {

    @Test
    void parentAuthorizationAndChildDownstreamPolicyAreIndependent() {
        assertNull(DamageAdmissionController.triggerFailure(
                DamageRequestKind.PROC,
                DamageTriggerPolicy.ALL_ALLOWED
        ));
        assertEquals(
                DamageFailureReason.PROC_SUPPRESSED,
                DamageAdmissionController.triggerFailure(
                        DamageRequestKind.PROC,
                        DamageTriggerPolicy.PROC_SUPPRESSED
                )
        );
        assertEquals(
                DamageTriggerPolicy.PROC_SUPPRESSED,
                DamageTriggerPolicy.defaultsFor(DamageRequestKind.PROC)
        );

        assertEquals(
                DamageFailureReason.REFLECTION_SUPPRESSED,
                DamageAdmissionController.triggerFailure(
                        DamageRequestKind.REFLECTED,
                        DamageTriggerPolicy.REFLECTION_SUPPRESSED
                )
        );
        assertEquals(
                DamageFailureReason.THORNS_SUPPRESSED,
                DamageAdmissionController.triggerFailure(
                        DamageRequestKind.THORNS,
                        DamageTriggerPolicy.THORNS_SUPPRESSED
                )
        );
        assertNull(DamageAdmissionController.triggerFailure(
                DamageRequestKind.DOT,
                DamageTriggerPolicy.NONE_ALLOWED
        ));
        assertNull(DamageAdmissionController.triggerFailure(
                DamageRequestKind.CUSTOM,
                DamageTriggerPolicy.NONE_ALLOWED
        ));
    }

    @Test
    void depthBoundaryAllowsMaximumAndRejectsOnlyOnePastIt() {
        assertNull(DamageAdmissionController.depthFailure(4, 5));
        assertNull(DamageAdmissionController.depthFailure(5, 5));
        assertEquals(
                DamageFailureReason.MAX_RECURSION_DEPTH,
                DamageAdmissionController.depthFailure(6, 5)
        );
    }

    @Test
    void siblingsShareRootBudgetAndTickFailureDoesNotConsumeIt() {
        DamageSafetySettings rootLimited =
                new DamageSafetySettings(5, 1, 10);
        DamageServerTickBudget state = new DamageServerTickBudget();
        DamageLineage root = DamageLineage.newRoot();
        DamageLineage first = root.newChild();
        DamageLineage sibling = root.newChild();

        assertTrue(state.tryAdmit(root, 10, rootLimited).admitted());
        assertTrue(state.tryAdmit(first, 10, rootLimited).admitted());
        DamageAdmissionResult rejected = state.tryAdmit(
                sibling,
                10,
                rootLimited
        );
        assertEquals(
                DamageFailureReason.ROOT_DERIVATION_LIMIT,
                rejected.reason()
        );
        assertEquals(1, root.derivedRequestCountInternal());
        assertEquals(2, state.count(10));

        DamageSafetySettings tickLimited =
                new DamageSafetySettings(5, 10, 1);
        DamageServerTickBudget tickState = new DamageServerTickBudget();
        DamageLineage tickRoot = DamageLineage.newRoot();
        DamageLineage tickChild = tickRoot.newChild();
        assertTrue(tickState.tryAdmit(tickRoot, 20, tickLimited).admitted());
        assertEquals(
                DamageFailureReason.SERVER_TICK_BUDGET_EXHAUSTED,
                tickState.tryAdmit(tickChild, 20, tickLimited).reason()
        );
        assertEquals(0, tickRoot.derivedRequestCountInternal());
        assertTrue(tickState.tryAdmit(tickChild, 21, tickLimited).admitted());
    }

    @Test
    void serverTickStatesAreIsolatedAndUseNewLimitsImmediately() {
        DamageSafetySettings twoPerTick =
                new DamageSafetySettings(5, 10, 2);
        DamageSafetySettings onePerTick =
                new DamageSafetySettings(5, 10, 1);
        DamageServerTickBudget firstServer = new DamageServerTickBudget();
        DamageServerTickBudget secondServer = new DamageServerTickBudget();

        assertTrue(firstServer.tryAdmit(
                DamageLineage.newRoot(),
                7,
                twoPerTick
        ).admitted());
        assertTrue(secondServer.tryAdmit(
                DamageLineage.newRoot(),
                7,
                onePerTick
        ).admitted());
        assertEquals(
                DamageFailureReason.SERVER_TICK_BUDGET_EXHAUSTED,
                firstServer.tryAdmit(
                        DamageLineage.newRoot(),
                        7,
                        onePerTick
                ).reason()
        );
        assertEquals(1, firstServer.count(7));
        assertEquals(1, secondServer.count(7));
    }

    @Test
    void rootBudgetIsReferenceOwnedAndExcludedFromLineageValueContract()
            throws Exception {
        DamageLineage root = DamageLineage.newRoot();
        DamageLineage child = root.newChild();
        assertTrue(child.reserveDerivedRequestInternal(4));
        assertEquals(1, root.derivedRequestCountInternal());

        assertFalse(Arrays.stream(DamageLineage.class.getDeclaredFields())
                .anyMatch(field -> Modifier.isStatic(field.getModifiers())
                        && Map.class.isAssignableFrom(field.getType())));
        assertFalse(root.toString().contains("Budget"));

        Class<?> budgetType = Class.forName(
                "io.github.naimjeg.damagenexus.api.damage."
                        + "DamageRootDerivationBudget"
        );
        var budgetConstructor = budgetType.getDeclaredConstructor();
        budgetConstructor.setAccessible(true);
        var lineageConstructor = DamageLineage.class.getDeclaredConstructor(
                long.class,
                long.class,
                OptionalLong.class,
                int.class,
                budgetType
        );
        lineageConstructor.setAccessible(true);
        DamageLineage first = lineageConstructor.newInstance(
                9_001L,
                9_001L,
                OptionalLong.empty(),
                0,
                budgetConstructor.newInstance()
        );
        DamageLineage second = lineageConstructor.newInstance(
                9_001L,
                9_001L,
                OptionalLong.empty(),
                0,
                budgetConstructor.newInstance()
        );
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

    }
}
