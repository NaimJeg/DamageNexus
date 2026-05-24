package io.github.naimjeg.damagenexus.builtin.processor;

import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.builtin.rule.provider.ItemDamageRuleProvider;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleProviders;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleExecutionProcessorContextBoundaryTest {

    @Test
    void externalProviderReceivesRestrictedContext() {
        DamageRuleContext internal = context();
        AtomicReference<DamageRuleContext> received = new AtomicReference<>();
        DamageRuleProvider external = (ctx, phase, out) ->
                received.set(ctx);

        DamageRuleContext callback =
                RuleExecutionProcessor.contextForProviderCallback(
                        external,
                        internal
                );
        external.collect(
                callback,
                DamagePhase.BASE_MODIFICATION,
                new ArrayList<>()
        );

        assertNotSame(internal, callback);
        assertSame(callback, received.get());
        assertFalse(callback instanceof DamageNexusContext);
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageInternalContexts.require(
                        callback,
                        "external provider test"
                )
        );
    }

    @Test
    void builtInIdentityCannotBeForgedByRegisteringSameClass() {
        DamageRuleProvider builtIn = DamageRuleProviders.all()
                .stream()
                .filter(ItemDamageRuleProvider.class::isInstance)
                .findFirst()
                .orElseThrow();
        DamageRuleProvider impersonator = new ItemDamageRuleProvider();
        DamageRuleContext internal = context();

        assertTrue(DamageRuleProviders.isBuiltin(builtIn));
        assertFalse(DamageRuleProviders.isBuiltin(impersonator));
        assertSame(
                internal,
                RuleExecutionProcessor.contextForProviderCallback(
                        builtIn,
                        internal
                )
        );

        DamageRuleContext restricted =
                RuleExecutionProcessor.contextForProviderCallback(
                        impersonator,
                        internal
                );

        assertNotSame(internal, restricted);
        assertThrows(
                IllegalArgumentException.class,
                () -> impersonator.collect(
                        restricted,
                        DamagePhase.BASE_MODIFICATION,
                        new ArrayList<>()
                )
        );
    }

    @Test
    void providerSnapshotCannotBeModified() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamageRuleProviders.all().add(
                        (ctx, phase, out) -> {
                        }
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
