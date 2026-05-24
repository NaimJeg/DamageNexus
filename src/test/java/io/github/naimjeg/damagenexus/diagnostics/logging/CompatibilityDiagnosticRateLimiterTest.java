package io.github.naimjeg.damagenexus.diagnostics.logging;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityDiagnosticRateLimiterTest {

    @Test
    void windowResetReportsSuppressedCount() {
        AtomicLong clock = new AtomicLong();
        CompatibilityDiagnosticRateLimiter limiter = limiter(clock, 2, 10, 1);

        assertTrue(limiter.decide("key").allowed());
        assertFalse(limiter.decide("key").allowed());
        assertFalse(limiter.decide("key").allowed());

        clock.set(10);
        var decision = limiter.decide("key");
        assertTrue(decision.allowed());
        assertEquals(2, decision.suppressed());
        assertEquals(" suppressed_in_previous_window=2", decision.suffix());
    }

    @Test
    void clockRewindDoesNotResetWindow() {
        AtomicLong clock = new AtomicLong(20);
        CompatibilityDiagnosticRateLimiter limiter = limiter(clock, 2, 10, 1);

        assertTrue(limiter.decide("key").allowed());
        clock.set(5);
        assertFalse(limiter.decide("key").allowed());
        assertEquals(1, limiter.size());
    }

    @Test
    void capacityUsesLruEviction() {
        AtomicLong clock = new AtomicLong();
        CompatibilityDiagnosticRateLimiter limiter = limiter(clock, 2, 10, 1);

        assertTrue(limiter.decide("a").allowed());
        assertTrue(limiter.decide("b").allowed());
        assertFalse(limiter.decide("a").allowed());
        assertTrue(limiter.decide("c").allowed());
        assertEquals(2, limiter.size());
        assertTrue(limiter.decide("b").allowed());
    }

    @Test
    void concurrentChecksRespectExactWindowLimitAndClear() {
        AtomicLong clock = new AtomicLong();
        CompatibilityDiagnosticRateLimiter limiter = limiter(clock, 8, 10, 5);

        long allowed = IntStream.range(0, 100)
                .parallel()
                .mapToObj(ignored -> limiter.decide("shared"))
                .filter(CompatibilityDiagnosticRateLimiter.Decision::allowed)
                .count();

        assertEquals(5, allowed);
        assertEquals(1, limiter.size());
        limiter.clearEntries();
        assertEquals(0, limiter.size());
    }

    private static CompatibilityDiagnosticRateLimiter limiter(
            AtomicLong clock,
            int capacity,
            long windowNanos,
            int maxPerWindow
    ) {
        return new CompatibilityDiagnosticRateLimiter(
                capacity,
                Duration.ofNanos(windowNanos),
                maxPerWindow,
                clock::get
        );
    }
}
