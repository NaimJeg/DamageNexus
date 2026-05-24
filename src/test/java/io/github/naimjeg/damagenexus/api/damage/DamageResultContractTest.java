package io.github.naimjeg.damagenexus.api.damage;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageResultContractTest {

    @Test
    void rejectedAndUnobservedFailedResultsHaveNoSettlement() {
        DamageRequest request = request();
        DamageResult rejected = DamageResult.rejected(
                request,
                DamageFailureReason.EQUIPMENT_OWNER_UNAUTHORIZED,
                "rejected"
        );
        DamageResult failed = DamageResult.failed(
                request,
                DamageFailureReason.PIPELINE_NOT_OBSERVED,
                "unobserved",
                false
        );

        assertEquals(DamageSubmissionStatus.REJECTED, rejected.status());
        assertFalse(rejected.settlement().isPresent());
        assertEquals(DamageSubmissionStatus.FAILED, failed.status());
        assertFalse(failed.settlement().isPresent());
    }

    @Test
    void privateConstructorRejectsAppliedWithoutSettlement() throws Exception {
        Constructor<DamageResult> constructor = DamageResult.class
                .getDeclaredConstructor(
                        DamageRequest.class,
                        DamageSubmissionStatus.class,
                        DamageFailure.class,
                        boolean.class,
                        boolean.class,
                        float.class,
                        boolean.class,
                        boolean.class,
                        DamageSettlementSnapshot.class
                );
        constructor.setAccessible(true);

        InvocationTargetException thrown = assertThrows(
                InvocationTargetException.class,
                () -> constructor.newInstance(
                        request(),
                        DamageSubmissionStatus.APPLIED,
                        null,
                        true,
                        true,
                        1.0f,
                        false,
                        false,
                        null
                )
        );
        assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
    }

    private static DamageRequest request() {
        return allocate(DamageRequest.class);
    }

    private static <T> T allocate(Class<T> type) {
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
            throw new AssertionError(exception);
        }
    }
}
