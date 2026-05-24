package io.github.naimjeg.damagenexus.core.settlement;

import io.github.naimjeg.damagenexus.api.damage.DamageParentRef;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.api.event.DamageSettledEvent;
import io.github.naimjeg.damagenexus.api.event.DamageSettlementCallback;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageSettlementEventPublisherTest {

    @AfterEach
    void clearDiagnosticLimiter() {
        io.github.naimjeg.damagenexus.diagnostics.logging
                .DamageNexusDiagnosticState.clearAll();
    }

    @Test
    void neoForgeObserverFailureDoesNotRollBackCommittedSettlement() {
        assertDoesNotThrow(() -> DamageSettlementEventPublisher.dispatch(
                42L,
                () -> {
                    throw new IllegalStateException("ordinary observer");
                }
        ));
    }

    @Test
    void neoForgeBusAbortsListenersAfterTheThrowingListener() {
        var bus = BusBuilder.builder().build();
        AtomicInteger laterInvocations = new AtomicInteger();
        bus.addListener(
                EventPriority.HIGH,
                TestEvent.class,
                event -> {
                    throw new IllegalStateException("intentional");
                }
        );
        bus.addListener(
                EventPriority.LOW,
                TestEvent.class,
                event -> laterInvocations.incrementAndGet()
        );

        assertThrows(IllegalStateException.class,
                () -> bus.post(new TestEvent()));
        assertEquals(0, laterInvocations.get());
    }

    @Test
    void ordinaryCodeCannotConstructAFrameworkCallback() {
        assertThrows(SecurityException.class,
                () -> DamageSettlementCallback.createInternal(null, null));
    }

    @Test
    void fatalObserverFailuresEscapeThePublicationBoundary() {
        LinkageError linkage = new LinkageError("synthetic");

        assertSame(linkage, assertThrows(
                LinkageError.class,
                () -> DamageSettlementEventPublisher.dispatch(
                        1L,
                        () -> {
                            throw linkage;
                        }
                )
        ));

        OutOfMemoryError memory = new OutOfMemoryError("synthetic");

        assertSame(memory, assertThrows(
                OutOfMemoryError.class,
                () -> DamageSettlementEventPublisher.dispatch(
                        2L,
                        () -> {
                            throw memory;
                        }
                )
        ));
    }

    @Test
    void callbackAuthorityUsesExactDeliveryRefAndServerIdentity() {
        DamageSettlementCallback callback = allocateWithoutConstructor(
                DamageSettlementCallback.class
        );
        DamageSettlementCallback otherCallback = allocateWithoutConstructor(
                DamageSettlementCallback.class
        );
        DamageParentRef authority = allocateWithoutConstructor(
                DamageParentRef.class
        );
        DamageParentRef otherAuthority = allocateWithoutConstructor(
                DamageParentRef.class
        );
        Object server = new Object();
        Object otherServer = new Object();

        try (DamageSettlementDispatchScope.Scope ignored =
                     DamageSettlementDispatchScope.openCallback(
                             callback,
                             authority,
                             server
                     )) {
            assertEquals(1, DamageSettlementDispatchScope.depthForTests());
            assertTrue(DamageSettlementDispatchScope.exposesAuthority(
                    callback,
                    authority,
                    server
            ));
            assertTrue(DamageSettlementDispatchScope.accepts(
                    authority,
                    server
            ));
            assertFalse(DamageSettlementDispatchScope.exposesAuthority(
                    otherCallback,
                    authority,
                    server
            ));
            assertFalse(DamageSettlementDispatchScope.accepts(
                    otherAuthority,
                    server
            ));
            assertFalse(DamageSettlementDispatchScope.accepts(
                    authority,
                    otherServer
            ));
            assertFalse(DamageSettlementDispatchScope.accepts(null, server));
        }

        assertEquals(0, DamageSettlementDispatchScope.depthForTests());
        assertFalse(DamageSettlementDispatchScope.accepts(authority, server));

        DamageSettlementDispatchScope.Scope observation =
                DamageSettlementDispatchScope.openObservation(
                        new Object(), server);
        assertFalse(DamageSettlementDispatchScope.accepts(authority, server));
        assertFalse(DamageSettlementDispatchScope.exposesAuthority(
                callback, authority, server));
        observation.close();
        observation.close();
        assertEquals(0, DamageSettlementDispatchScope.depthForTests());
    }

    @Test
    void eachCallbackInvocationCreatesANewAuthorityIdentity() {
        DamageOrigin origin = allocateWithoutConstructor(DamageOrigin.class);

        DamageParentRef first =
                DamageSettlementCallbacks.createAuthorityForInvocation(origin);
        DamageParentRef second =
                DamageSettlementCallbacks.createAuthorityForInvocation(origin);

        assertNotSame(first, second);
    }

    @Test
    void callbackDispatchRejectsAnUnfrozenRegistry() {
        DamageSettlementCallbacks.resetForTesting();
        try {
            var snapshot = allocateWithoutConstructor(
                    io.github.naimjeg.damagenexus.api.damage
                            .DamageSettlementSnapshot.class);
            assertThrows(IllegalStateException.class,
                    () -> DamageSettlementCallbacks.dispatch(snapshot));
        } finally {
            DamageSettlementCallbacks.resetForTesting();
        }
    }

    @Test
    void neoForgeObservationEventHasNoAuthorityApiOrState() {
        assertFalse(java.util.Arrays.stream(
                        DamageSettledEvent.class.getDeclaredFields())
                .anyMatch(field -> field.getType() == DamageParentRef.class));
        assertThrows(NoSuchMethodException.class,
                () -> DamageSettledEvent.class.getMethod("childAuthority"));
        assertThrows(NoSuchMethodException.class,
                () -> DamageSettledEvent.class.getMethod(
                        "createInternal",
                        io.github.naimjeg.damagenexus.api.damage
                                .DamageSettlementSnapshot.class,
                        DamageParentRef.class
                ));
    }

    private static <T> T allocateWithoutConstructor(Class<T> type) {
        try {
            Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
            Field field = unsafeType.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            Method allocate = unsafeType.getMethod(
                    "allocateInstance",
                    Class.class
            );
            return type.cast(allocate.invoke(unsafe, type));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to allocate identity fixture", exception);
        }
    }

    private static final class TestEvent extends Event {
    }
}
