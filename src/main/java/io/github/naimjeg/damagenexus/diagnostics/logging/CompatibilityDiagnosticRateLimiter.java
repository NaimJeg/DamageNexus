package io.github.naimjeg.damagenexus.diagnostics.logging;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Fixed-capacity compatibility limiter driven exclusively by monotonic time. */
final class CompatibilityDiagnosticRateLimiter {

    private static final int DEFAULT_MAX_KEYS = 128;
    private static final Duration DEFAULT_WINDOW = Duration.ofSeconds(5);
    private static final int DEFAULT_MAX_PER_WINDOW = 5;
    private static final CompatibilityDiagnosticRateLimiter DEFAULT =
            new CompatibilityDiagnosticRateLimiter(
                    DEFAULT_MAX_KEYS,
                    DEFAULT_WINDOW,
                    DEFAULT_MAX_PER_WINDOW,
                    System::nanoTime
            );

    private final int maxKeys;
    private final long windowNanos;
    private final int maxPerWindow;
    private final LongSupplier clock;
    private final Map<String, Entry> entries =
            new LinkedHashMap<>(16, 0.75f, true);

    CompatibilityDiagnosticRateLimiter(
            int maxKeys,
            Duration window,
            int maxPerWindow,
            LongSupplier clock
    ) {
        if (maxKeys <= 0 || maxPerWindow <= 0) {
            throw new IllegalArgumentException("limits must be positive");
        }
        Objects.requireNonNull(window, "window");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.maxKeys = maxKeys;
        this.windowNanos = window.toNanos();
        this.maxPerWindow = maxPerWindow;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    static Decision check(String key) {
        return DEFAULT.decide(key);
    }

    synchronized Decision decide(String key) {
        long now = clock.getAsLong();
        String safeKey = key == null || key.isBlank() ? "unknown" : key;
        Entry entry = entries.get(safeKey);

        if (entry == null) {
            evictIfNeeded();
            entries.put(safeKey, new Entry(now, 1, 0));
            return new Decision(true, 0);
        }

        long elapsed = now - entry.windowStartNanos;
        if (elapsed >= 0L && elapsed >= windowNanos) {
            int suppressed = entry.suppressed;
            entry.windowStartNanos = now;
            entry.emitted = 1;
            entry.suppressed = 0;
            return new Decision(true, suppressed);
        }

        if (entry.emitted < maxPerWindow) {
            entry.emitted++;
            return new Decision(true, 0);
        }

        entry.suppressed++;
        return new Decision(false, 0);
    }

    private void evictIfNeeded() {
        if (entries.size() < maxKeys) {
            return;
        }
        var iterator = entries.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    static void clear() {
        DEFAULT.clearEntries();
    }

    synchronized void clearEntries() {
        entries.clear();
    }

    synchronized int size() {
        return entries.size();
    }

    record Decision(boolean allowed, int suppressed) {
        String suffix() {
            return suppressed > 0
                    ? " suppressed_in_previous_window=" + suppressed
                    : "";
        }
    }

    private static final class Entry {
        private long windowStartNanos;
        private int emitted;
        private int suppressed;

        private Entry(long windowStartNanos, int emitted, int suppressed) {
            this.windowStartNanos = windowStartNanos;
            this.emitted = emitted;
            this.suppressed = suppressed;
        }
    }
}
