package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleExecutionDiagnosticsLogTest {

    @AfterEach
    void clear() {
        DamageNexusDiagnosticState.clearAll();
    }

    @Test
    void callbackErrorIsModeIndependentRateLimitedAndKeepsThrowableLast() {
        List<Object[]> invocations = new ArrayList<>();
        Logger logger = (Logger) Proxy.newProxyInstance(
                Logger.class.getClassLoader(),
                new Class<?>[]{Logger.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("error")) {
                        invocations.add(args);
                    }
                    return defaultValue(method.getReturnType());
                }
        );
        RuntimeException failure = new RuntimeException("boom");
        Identifier ruleId = Identifier.fromNamespaceAndPath("test", "rule");

        assertTrue(RuleExecutionDiagnosticsLog.error(
                logger,
                ruleId,
                DamagePhase.BASE_MODIFICATION,
                "condition/test:throwing",
                failure
        ));
        assertFalse(RuleExecutionDiagnosticsLog.error(
                logger,
                ruleId,
                DamagePhase.BASE_MODIFICATION,
                "condition/test:throwing",
                failure
        ));

        assertTrue(invocations.size() == 1);
        Object[] loggerArguments = (Object[]) invocations.getFirst()[1];
        assertSame(failure, loggerArguments[loggerArguments.length - 1]);
    }

    @Test
    void operationAndExceptionClassUseIndependentStableKeys() {
        Logger logger = noOpLogger();
        Identifier ruleId = Identifier.fromNamespaceAndPath("test", "rule");

        assertTrue(RuleExecutionDiagnosticsLog.error(
                logger, ruleId, DamagePhase.BASE_MODIFICATION,
                "operation/test:op", new IllegalStateException()
        ));
        assertTrue(RuleExecutionDiagnosticsLog.error(
                logger, ruleId, DamagePhase.BASE_MODIFICATION,
                "operation/test:op", new IllegalArgumentException()
        ));
    }

    private static Logger noOpLogger() {
        return (Logger) Proxy.newProxyInstance(
                Logger.class.getClassLoader(),
                new Class<?>[]{Logger.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }
}
