package io.github.naimjeg.damagenexus.core;

import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

/**
 * Compact additive pre-multiplier accumulator.
 * <p>
 * DamageNexus pre multipliers use bucketed additive stacking:
 * all values written to the same pre-multiplier bucket are added first,
 * then each bucket is applied multiplicatively.
 */
public final class PreMultiplierSet {

    private float[] values;

    @ApiStatus.Internal
    public PreMultiplierSet copy() {
        PreMultiplierSet copy = new PreMultiplierSet();
        copy.values = values == null
                ? null
                : Arrays.copyOf(values, values.length);
        return copy;
    }

    public static float applyCombined(
            float amount,
            PreMultiplierSet first,
            PreMultiplierSet second
    ) {
        if ((first == null || first.isEmpty())
                && (second == null || second.isEmpty())) {
            return amount;
        }

        PreMultiplierBucketRegistry.requireFrozen();

        int count = PreMultiplierBucketRegistry.bucketCount();
        for (int i = 0; i < count; i++) {
            float value = 0.0f;

            if (first != null) {
                value += first.getOrZero(i);
            }

            if (second != null) {
                value += second.getOrZero(i);
            }

            if (value != 0.0f) {
                amount *= 1.0f + value;
            }
        }

        return amount;
    }

    private static void validateId(int preMultiplierBucketId) {
        int count = PreMultiplierBucketRegistry.bucketCount();

        if (preMultiplierBucketId < 0 || preMultiplierBucketId >= count) {
            throw new IndexOutOfBoundsException(
                    "Invalid pre-multiplier bucket id: " + preMultiplierBucketId
            );
        }
    }

    public void add(int preMultiplierBucketId, float value) {
        PreMultiplierBucketRegistry.requireFrozen();
        validateId(preMultiplierBucketId);

        if (value == 0.0f) {
            return;
        }

        ensureCapacity();
        values[preMultiplierBucketId] += value;
    }

    public float getOrZero(int preMultiplierBucketId) {
        if (values == null
                || preMultiplierBucketId < 0
                || preMultiplierBucketId >= values.length) {
            return 0.0f;
        }

        return values[preMultiplierBucketId];
    }

    public boolean isEmpty() {
        if (values == null) {
            return true;
        }

        int count = Math.min(values.length, PreMultiplierBucketRegistry.bucketCount());
        for (int i = 0; i < count; i++) {
            if (values[i] != 0.0f) {
                return false;
            }
        }

        return true;
    }

    public float apply(float amount) {
        if (values == null) {
            return amount;
        }

        int count = Math.min(values.length, PreMultiplierBucketRegistry.bucketCount());
        for (int i = 0; i < count; i++) {
            float value = values[i];

            if (value != 0.0f) {
                amount *= 1.0f + value;
            }
        }

        return amount;
    }

    private void ensureCapacity() {
        int count = PreMultiplierBucketRegistry.bucketCount();

        if (values != null && values.length == count) {
            return;
        }

        float[] next = new float[count];

        if (values != null) {
            System.arraycopy(values, 0, next, 0, Math.min(values.length, next.length));
        }

        values = next;
    }
}

