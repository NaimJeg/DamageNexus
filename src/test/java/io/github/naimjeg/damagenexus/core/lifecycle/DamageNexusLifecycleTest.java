package io.github.naimjeg.damagenexus.core.lifecycle;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegisterEvent;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegistrar;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AlwaysCondition;
import io.github.naimjeg.damagenexus.builtin.rule.operation.CancelDamageOperation;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.damagenexus.core.rule.DatapackDamageRuleStore;
import io.github.naimjeg.damagenexus.core.rule.DatapackDamageRuleReloadListener;
import io.github.naimjeg.damagenexus.registry.DamagePhaseProcessorRegistry;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleConditionTypes;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleOperationTypes;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleProviders;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.EventBusSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNexusLifecycleTest {

    private DamageNexusRegistrationAccess access;

    @AfterEach
    void resetLifecycle() {
        if (access != null) {
            access.close();
        }
        DamageNexusLifecycle.resetForTesting();
    }

    @Test
    void legalLifecycleIsForwardOnly() {
        assertEquals(
                DamageNexusLifecycleState.CONSTRUCTING,
                DamageNexusLifecycle.state()
        );

        access = DamageNexusLifecycle.beginRegistering();
        assertEquals(
                DamageNexusLifecycleState.REGISTERING,
                DamageNexusLifecycle.state()
        );

        DamageNexusLifecycle.freezeRegistration(access);
        assertEquals(
                DamageNexusLifecycleState.FROZEN,
                DamageNexusLifecycle.state()
        );

        DamageNexusLifecycle.running();
        assertEquals(
                DamageNexusLifecycleState.RUNNING,
                DamageNexusLifecycle.state()
        );
    }

    @Test
    void allIllegalTransitionsAreRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> DamageNexusLifecycle.freezeRegistration(null)
        );
        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::running
        );

        access = DamageNexusLifecycle.beginRegistering();

        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::beginRegistering
        );
        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::running
        );

        DamageNexusLifecycle.freezeRegistration(access);

        assertThrows(
                IllegalStateException.class,
                () -> DamageNexusLifecycle.freezeRegistration(access)
        );

        DamageNexusLifecycle.running();

        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::beginRegistering
        );
        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::running
        );
    }

    @Test
    void registrationFailureIsTerminalAndClosesCapability() {
        access = DamageNexusLifecycle.beginRegistering();
        DamageNexusRegistrationSession session =
                new DamageNexusRegistrationSession(access);
        DamageNexusRegistrar saved =
                new DamageNexusRegisterEvent(session).registrar();

        DamageNexusLifecycle.failBootstrap(access);

        assertEquals(
                DamageNexusLifecycleState.FAILED,
                DamageNexusLifecycle.state()
        );
        assertFalse(access.isActive());
        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::beginRegistering
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamageNexusLifecycle.freezeRegistration(access)
        );
        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::running
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleProviders.register(
                        access,
                        (ctx, phase, out) -> {
                        }
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> saved.registerRuleProvider(
                        (ctx, phase, out) -> {
                        }
                )
        );
    }

    @Test
    void postFreezeBootstrapFailureAlsoBecomesTerminal() {
        access = DamageNexusLifecycle.beginRegistering();
        DamageNexusLifecycle.freezeRegistration(access);

        DamageNexusLifecycle.failBootstrap(access);

        assertEquals(
                DamageNexusLifecycleState.FAILED,
                DamageNexusLifecycle.state()
        );
        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::running
        );
        assertThrows(
                IllegalStateException.class,
                DamageNexusLifecycle::beginRegistering
        );
    }

    @Test
    void failureTransitionCannotBeForgedOutsideBootstrapWindow() {
        assertThrows(
                IllegalStateException.class,
                () -> DamageNexusLifecycle.failBootstrap(null)
        );

        access = DamageNexusLifecycle.beginRegistering();
        DamageNexusLifecycle.freezeRegistration(access);
        DamageNexusLifecycle.running();

        assertThrows(
                IllegalStateException.class,
                () -> DamageNexusLifecycle.failBootstrap(access)
        );
        assertEquals(
                DamageNexusLifecycleState.RUNNING,
                DamageNexusLifecycle.state()
        );
    }

    @Test
    void registrarWorksOnlyDuringEventCallbackWindow() {
        access = DamageNexusLifecycle.beginRegistering();
        DamageNexusRegistrationSession session =
                new DamageNexusRegistrationSession(access);
        DamageNexusRegisterEvent event =
                new DamageNexusRegisterEvent(session);
        DamageNexusRegistrar saved = event.registrar();
        DamageRuleProvider provider = (ctx, phase, out) -> {
        };
        DamagePhaseProcessor processor = new TestProcessor();
        Identifier conditionId =
                Identifier.fromNamespaceAndPath(
                        "test",
                        "session_condition"
                );
        Identifier operationId =
                Identifier.fromNamespaceAndPath(
                        "test",
                        "session_operation"
                );
        Identifier bucketId =
                Identifier.fromNamespaceAndPath(
                        "test",
                        "session_bucket"
                );

        event.registerCondition(conditionId, AlwaysCondition.CODEC);
        event.registerOperation(operationId, CancelDamageOperation.CODEC);
        event.registerRuleProvider(provider);
        event.registerPhaseProcessor(processor);
        event.registerPreMultiplierBucket(bucketId);

        assertTrue(DamageRuleConditionTypes.registeredTypes()
                .contains(conditionId));
        assertTrue(DamageRuleOperationTypes.registeredTypes()
                .contains(operationId));
        assertTrue(DamageRuleProviders.all().contains(provider));
        assertTrue(DamagePhaseProcessorRegistry.externalProcessors()
                .contains(processor));
        assertTrue(PreMultiplierBucketRegistry.registeredBuckets()
                .contains(bucketId));

        session.close();

        assertThrows(
                IllegalStateException.class,
                () -> saved.registerRuleProvider(
                        (ctx, phase, out) -> {
                        }
                )
        );

        DamageNexusLifecycle.freezeRegistration(access);
        DamageNexusLifecycle.running();

        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleProviders.register(
                        access,
                        (ctx, phase, out) -> {
                        }
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamagePhaseProcessorRegistry.registerExternal(
                        access,
                        new TestProcessor()
                )
        );
    }

    @Test
    void registryWritesBeforeEventRequireUnforgeableCapability() {
        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleConditionTypes.register(
                        null,
                        Identifier.fromNamespaceAndPath(
                                "test",
                                "outside_event_condition"
                        ),
                        AlwaysCondition.CODEC
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleOperationTypes.register(
                        null,
                        Identifier.fromNamespaceAndPath(
                                "test",
                                "outside_event_operation"
                        ),
                        CancelDamageOperation.CODEC
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleProviders.register(
                        null,
                        (ctx, phase, out) -> {
                        }
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamagePhaseProcessorRegistry.registerExternal(
                        null,
                        new TestProcessor()
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> PreMultiplierBucketRegistry
                        .registerPreMultiplierBucket(
                                null,
                                Identifier.fromNamespaceAndPath(
                                        "test",
                                        "outside_event"
                                )
                        )
        );
    }

    @Test
    void externalCodeCannotFreezeRegistryEarly() {
        assertFalse(PreMultiplierBucketRegistry.isFrozen());

        assertThrows(
                IllegalStateException.class,
                () -> PreMultiplierBucketRegistry.freeze(null)
        );

        assertFalse(PreMultiplierBucketRegistry.isFrozen());
    }

    @Test
    void registryReadSnapshotsAreImmutable() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamageRuleProviders.all().clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamagePhaseProcessorRegistry
                        .externalProcessors()
                        .clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamageRuleConditionTypes
                        .registeredTypes()
                        .clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamageRuleOperationTypes
                        .registeredTypes()
                        .clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> PreMultiplierBucketRegistry
                        .registeredBuckets()
                        .clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> DatapackDamageRuleStore.rules().clear()
        );
    }

    @Test
    void lifecycleProgressionAndCapabilityConstructionAreNotPublic()
            throws Exception {
        assertNotPublic(DamageNexusLifecycle.class.getDeclaredMethod(
                "beginRegistering"
        ));
        assertNotPublic(DamageNexusLifecycle.class.getDeclaredMethod(
                "freezeRegistration",
                DamageNexusRegistrationAccess.class
        ));
        assertNotPublic(DamageNexusLifecycle.class.getDeclaredMethod(
                "running"
        ));
        assertNotPublic(DamageNexusLifecycle.class.getDeclaredMethod(
                "failBootstrap",
                DamageNexusRegistrationAccess.class
        ));
        assertThrows(
                NoSuchMethodException.class,
                () -> DamageNexusLifecycle.class.getDeclaredMethod(
                        "beginReloading"
                )
        );

        assertTrue(Arrays.stream(
                        DamageNexusRegistrationAccess.class
                                .getDeclaredConstructors()
                )
                .noneMatch(constructor -> Modifier.isPublic(
                        constructor.getModifiers()
                )));
    }

    @Test
    void bootstrapIsAFrameworkOwnedModBusSubscriber() {
        assertFalse(Modifier.isPublic(
                DamageNexusBootstrap.class.getModifiers()
        ));
        assertTrue(Arrays.stream(
                        DamageNexusBootstrap.class.getDeclaredMethods()
                )
                .noneMatch(method -> method.getName().equals("install")));

        EventBusSubscriber annotation =
                DamageNexusBootstrap.class.getAnnotation(
                        EventBusSubscriber.class
                );

        assertTrue(annotation != null);
        assertEquals(DamageNexus.MODID, annotation.modid());
    }

    @Test
    void commonSetupCanOnlyBeClaimedOncePerProcess() {
        assertTrue(DamageNexusBootstrap.claimCommonSetup());
        assertFalse(DamageNexusBootstrap.claimCommonSetup());
    }

    @Test
    void publicRegistryMutationsAllRequireRegistrationCapability() {
        assertCapabilityProtectedMutation(
                DamageRuleProviders.class,
                "register"
        );
        assertCapabilityProtectedMutation(
                DamagePhaseProcessorRegistry.class,
                "registerExternal"
        );
        assertCapabilityProtectedMutation(
                DamageRuleConditionTypes.class,
                "register"
        );
        assertCapabilityProtectedMutation(
                DamageRuleOperationTypes.class,
                "register"
        );
        assertCapabilityProtectedMutation(
                PreMultiplierBucketRegistry.class,
                "registerPreMultiplierBucket"
        );
        assertCapabilityProtectedMutation(
                PreMultiplierBucketRegistry.class,
                "freeze"
        );
        assertCapabilityProtectedMutation(
                PreMultiplierBuckets.class,
                "register"
        );
    }

    @Test
    void datapackRegistryMutationMethodsAreNotPublic() {
        assertTrue(Arrays.stream(DamageChannelRegistry.class
                        .getDeclaredMethods())
                .filter(method -> method.getName().equals("apply")
                        || method.getName().equals(
                        "replaceStateForTesting"
                )
                        || method.getName().equals(
                        "resetStateForTesting"
                ))
                .noneMatch(method -> Modifier.isPublic(
                        method.getModifiers()
                )));
        assertTrue(Arrays.stream(DatapackDamageRuleStore.class
                        .getDeclaredMethods())
                .filter(method -> method.getName().equals("replace"))
                .noneMatch(method -> Modifier.isPublic(
                        method.getModifiers()
                )));
    }

    @Test
    void reloadListenersRequireFrameworkOwnedCapability() {
        assertTrue(Arrays.stream(
                        DamageNexusReloadAccess.class
                                .getDeclaredConstructors()
                )
                .noneMatch(constructor -> Modifier.isPublic(
                        constructor.getModifiers()
                )));
        assertTrue(Arrays.stream(
                        DamageChannelRegistry.class
                                .getDeclaredConstructors()
                )
                .allMatch(constructor ->
                        constructor.getParameterCount() == 1
                                && constructor.getParameterTypes()[0]
                                == DamageNexusReloadAccess.class));
        assertTrue(Arrays.stream(
                        DatapackDamageRuleReloadListener.class
                                .getDeclaredConstructors()
                )
                .allMatch(constructor ->
                        constructor.getParameterCount() == 1
                                && constructor.getParameterTypes()[0]
                                == DamageNexusReloadAccess.class));
    }

    @Test
    void registerEventExposesTheExactExpiringRegistrar() {
        access = DamageNexusLifecycle.beginRegistering();
        DamageNexusRegistrationSession session =
                new DamageNexusRegistrationSession(access);
        DamageNexusRegisterEvent event =
                new DamageNexusRegisterEvent(session);

        assertSame(session, event.registrar());
        assertTrue(session.isOpen());

        session.close();

        assertFalse(session.isOpen());
    }

    private static void assertNotPublic(Method method) {
        assertFalse(Modifier.isPublic(method.getModifiers()));
    }

    private static void assertCapabilityProtectedMutation(
            Class<?> owner,
            String methodName
    ) {
        assertTrue(Arrays.stream(owner.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .allMatch(method -> method.getParameterCount() > 0
                        && method.getParameterTypes()[0]
                        == DamageNexusRegistrationAccess.class));
    }

    private static final class TestProcessor
            implements DamagePhaseProcessor {

        @Override
        public void apply(DamageRuleContext ctx) {
        }

        @Override
        public DamagePhase phase() {
            return DamagePhase.BASE_MODIFICATION;
        }
    }
}
