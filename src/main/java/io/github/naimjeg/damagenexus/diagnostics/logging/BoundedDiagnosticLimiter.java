package io.github.naimjeg.damagenexus.diagnostics.logging;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Fixed-capacity, time-expiring warn-once state. Access is synchronized so
 * capacity remains exact under concurrent damage processing.
 */
final class BoundedDiagnosticLimiter<K> {

    private final int capacity;
    private final long ttlNanos;
    private final LongSupplier clock;
    private final LinkedHashMap<K, Long> timestamps =
            new LinkedHashMap<>(16, 0.75f, true);

    BoundedDiagnosticLimiter(int capacity, Duration ttl) {
        this(capacity, ttl, System::nanoTime);
    }

    BoundedDiagnosticLimiter(
            int capacity,
            Duration ttl,
            LongSupplier clock
    ) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }

        Objects.requireNonNull(ttl, "ttl");

        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }

        this.capacity = capacity;
        this.ttlNanos = ttl.toNanos();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    synchronized boolean shouldLog(K key) {
        Objects.requireNonNull(key, "key");

        long now = clock.getAsLong();
        removeExpired(now);

        if (timestamps.get(key) != null) {
            return false;
        }

        if (timestamps.size() >= capacity) {
            Iterator<K> iterator = timestamps.keySet().iterator();

            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        timestamps.put(key, now);
        return true;
    }

    synchronized void clear() {
        timestamps.clear();
    }

    synchronized int size() {
        removeExpired(clock.getAsLong());
        return timestamps.size();
    }

    private void removeExpired(long now) {
        Iterator<Map.Entry<K, Long>> iterator =
                timestamps.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<K, Long> entry = iterator.next();

            if (now - entry.getValue() >= ttlNanos) {
                iterator.remove();
            }
        }
    }
}
