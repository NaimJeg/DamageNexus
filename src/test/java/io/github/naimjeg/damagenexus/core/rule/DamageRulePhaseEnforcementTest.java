package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleStacking;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleValidator;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRulePhaseEnforcementTest {

    private static final Identifier RULE_ID =
            Identifier.fromNamespaceAndPath("test", "invalid_cancel");

    @Test
    void directDefinitionIsRejectedBySharedValidator() {
        DamageRuleDefinition invalid = invalidCancelRule();

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> DamageRuleValidator.requireValid(
                        invalid,
                        "test/direct_definition"
                )
        );

        assertTrue(error.getMessage().contains("test/direct_definition"));
        assertTrue(error.getMessage().contains(RULE_ID.toString()));
        assertTrue(error.getMessage().contains("cancel_damage"));
        assertTrue(error.getMessage().contains("BASE_MODIFICATION"));
        assertTrue(error.getMessage().contains("FINAL_OVERRIDE"));
    }

    @Test
    void datapackReloadRejectsCancelOutsideFinalOverride() {
        assertFalse(DatapackDamageRuleReloadListener.validateRule(
                Identifier.fromNamespaceAndPath(
                        "test",
                        "invalid_cancel_file"
                ),
                invalidCancelRule()
        ));
    }

    @Test
    void runtimeGuardDoesNotInvokeInvalidCancelOperation() {
        AtomicBoolean cancelInvoked = new AtomicBoolean();
        DamageRuleContext delegate = context(cancelInvoked);

        DamageMutationResult result = DamageRuleExecutor.invokeOperation(
                delegate,
                DamagePhase.BASE_MODIFICATION,
                DamageNexusOperations.cancelDamage("test/runtime")
        );

        assertEquals(DamageMutationResult.REJECTED_WRONG_PHASE, result);
        assertFalse(cancelInvoked.get());
    }

    private static DamageRuleDefinition invalidCancelRule() {
        return new DamageRuleDefinition(
                RULE_ID,
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(),
                List.of(DamageNexusOperations.cancelDamage("test/cancel")),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static DamageRuleContext context(AtomicBoolean cancelInvoked) {
        return (DamageRuleContext) Proxy.newProxyInstance(
                DamageRuleContext.class.getClassLoader(),
                new Class<?>[]{DamageRuleContext.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("tryCancelDamage")) {
                        cancelInvoked.set(true);
                        return DamageMutationResult.APPLIED;
                    }

                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }

        if (type == boolean.class) {
            return false;
        }

        if (type == long.class) {
            return 0L;
        }

        if (type == int.class) {
            return 0;
        }

        if (type == float.class) {
            return 0.0f;
        }

        throw new IllegalStateException("Unsupported primitive: " + type);
    }
}
