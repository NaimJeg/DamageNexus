package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.api.damage.DamageRequest;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import net.minecraft.world.damagesource.DamageSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRequestSubmissionTrackerTest {

    @Test
    void submissionsRejectOutOfOrderCloseAndCleanUpInLifoOrder() {
        DamageRequest request = allocate(DamageRequest.class);
        DamageSource source = allocate(DamageSource.class);
        DamageOrigin origin = allocate(DamageOrigin.class);
        DamageAdmissionResult admission =
                DamageAdmissionResult.admitted(0, 1);
        DamageRequestSubmissionTracker.Submission outer =
                DamageRequestSubmissionTracker.open(
                        request,
                        source,
                        admission,
                        origin
                );
        DamageRequestSubmissionTracker.Submission inner =
                DamageRequestSubmissionTracker.open(
                        request,
                        source,
                        admission,
                        origin
                );

        assertTrue(DamageRequestSubmissionTracker.hasActiveSubmission());
        assertThrows(IllegalStateException.class, outer::close);
        inner.close();
        outer.close();
        assertFalse(DamageRequestSubmissionTracker.hasActiveSubmission());
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
