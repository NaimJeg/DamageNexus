package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRuleCallbackBoundaryTest {

    @Test
    void conditionAndOperationReceiveRestrictedContext() {
        DamageRuleContext internal = context();
        AtomicReference<DamageRuleContext> conditionContext =
                new AtomicReference<>();
        AtomicReference<DamageRuleContext> operationContext =
                new AtomicReference<>();
        DamageRuleCondition condition = new DamageRuleCondition() {
            @Override
            public Identifier type() {
                return Identifier.fromNamespaceAndPath(
                        "test",
                        "capturing_condition"
                );
            }

            @Override
            public boolean test(DamageRuleContext ctx) {
                conditionContext.set(ctx);
                return true;
            }
        };
        DamageRuleOperation operation = new DamageRuleOperation() {
            @Override
            public Identifier type() {
                return Identifier.fromNamespaceAndPath(
                        "test",
                        "capturing_operation"
                );
            }

            @Override
            public DamageMutationResult apply(DamageRuleContext ctx) {
                operationContext.set(ctx);
                return DamageMutationResult.NO_OP_ZERO;
            }
        };

        assertTrue(DamageRuleExecutor.invokeCondition(
                internal,
                condition,
                RuleExecutionContext.javaApiRule(DamageRuleRole.OFFENSIVE)
        ));
        assertEquals(
                DamageMutationResult.NO_OP_ZERO,
                DamageRuleExecutor.invokeOperation(
                        internal,
                        DamagePhase.BASE_MODIFICATION,
                        operation
                )
        );

        DamageRuleContext conditionReceived = conditionContext.get();
        DamageRuleContext operationReceived = operationContext.get();

        assertNotSame(internal, conditionReceived);
        assertNotSame(internal, operationReceived);
        assertFalse(conditionReceived instanceof DamageNexusContext);
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageInternalContexts.require(
                        conditionReceived,
                        "condition test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageInternalContexts.require(
                        operationReceived,
                        "operation test"
                )
        );
    }

    @Test
    void seriousJvmErrorsEscapeConditionBoundary() {
        DamageRuleContext internal = context();
        RuleExecutionContext execution =
                RuleExecutionContext.javaApiRule(
                        DamageRuleRole.OFFENSIVE
                );

        assertThrows(
                OutOfMemoryError.class,
                () -> DamageRuleExecutor.invokeCondition(
                        internal,
                        throwingCondition(new OutOfMemoryError("synthetic")),
                        execution
                )
        );
        assertThrows(
                StackOverflowError.class,
                () -> DamageRuleExecutor.invokeCondition(
                        internal,
                        throwingCondition(
                                new StackOverflowError("synthetic")
                        ),
                        execution
                )
        );
        assertThrows(
                LinkageError.class,
                () -> DamageRuleExecutor.invokeCondition(
                        internal,
                        throwingCondition(new LinkageError("synthetic")),
                        execution
                )
        );
    }

    private static DamageRuleContext context() {
        return (DamageRuleContext) Proxy.newProxyInstance(
                DamageRuleContext.class.getClassLoader(),
                new Class<?>[]{DamageRuleContext.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private static DamageRuleCondition throwingCondition(Error error) {
        return new DamageRuleCondition() {
            @Override
            public Identifier type() {
                return Identifier.fromNamespaceAndPath(
                        "test",
                        "throwing_condition"
                );
            }

            @Override
            public boolean test(DamageRuleContext ctx) {
                throw error;
            }
        };
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
