package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.critical.*;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;

class CriticalDecisionStateTest {
    @AfterEach
    void resetSampler() {
        CriticalDecisionEngine.resetSamplerForTesting();
    }

    @Test
    void highestPriorityLayerWinsAndSuppressWinsWithinThatLayer() {
        CriticalDecisionState state = new CriticalDecisionState();
        state.add(contribution("low_suppress", 1, CriticalDecision.SUPPRESS_CRITICAL));
        state.add(contribution("high_force", 9, CriticalDecision.FORCE_CRITICAL));
        assertEquals(CriticalDecision.FORCE_CRITICAL, state.resolveContributions());

        state.add(contribution("high_suppress", 9, CriticalDecision.SUPPRESS_CRITICAL));
        assertEquals(CriticalDecision.SUPPRESS_CRITICAL, state.resolveContributions());
    }

    @Test
    void freezeIsOneShotAndRejectsLateContributions() {
        CriticalDecisionState state = new CriticalDecisionState();
        assertTrue(state.claimProviderCollection());
        assertFalse(state.claimProviderCollection());
        state.add(contribution("force", 0, CriticalDecision.FORCE_CRITICAL));
        state.freeze(CriticalDecision.FORCE_CRITICAL, true,
                CriticalDecisionOutcome.FORCED, false);
        state.markEffectApplied();
        assertTrue(state.snapshot().frozen());
        assertTrue(state.snapshot().critical());
        assertFalse(state.snapshot().chanceSampled());
        assertThrows(IllegalStateException.class,
                () -> state.add(contribution("late", 100, CriticalDecision.SUPPRESS_CRITICAL)));
        assertThrows(IllegalStateException.class,
                () -> state.freeze(CriticalDecision.DEFAULT, false,
                        CriticalDecisionOutcome.DEFAULT_NON_CRITICAL, false));
        assertThrows(IllegalStateException.class, state::markEffectApplied);
    }

    @Test
    void checkpointRestoresContributionsFreezeOutcomeSampleAndEffect() {
        CriticalDecisionState state = new CriticalDecisionState();
        state.add(contribution("original", 2, CriticalDecision.FORCE_CRITICAL));
        CriticalDecisionState.Checkpoint checkpoint = state.checkpoint();
        assertTrue(state.claimProviderCollection());
        state.add(contribution("throwing_callback", 20, CriticalDecision.FORCE_CRITICAL));
        state.freeze(CriticalDecision.FORCE_CRITICAL, true,
                CriticalDecisionOutcome.FORCED, false);
        state.markEffectApplied();

        state.restore(checkpoint);
        assertFalse(state.snapshot().frozen());
        assertFalse(state.snapshot().chanceSampled());
        assertFalse(state.snapshot().effectApplied());
        assertEquals(1, state.orderedContributions().size());
        assertTrue(state.claimProviderCollection());
        assertEquals(CriticalDecision.FORCE_CRITICAL, state.resolveContributions());
    }

    @Test
    void chanceSanitizationIsFiniteAndBounded() {
        assertEquals(0.0f, CriticalDecisionEngine.sanitizeChance(Float.NaN));
        assertEquals(0.0f, CriticalDecisionEngine.sanitizeChance(Float.POSITIVE_INFINITY));
        assertEquals(0.0f, CriticalDecisionEngine.sanitizeChance(-1.0f));
        assertEquals(1.0f, CriticalDecisionEngine.sanitizeChance(2.0f));
        assertEquals(0.25f, CriticalDecisionEngine.sanitizeChance(0.25f));

        AtomicInteger calls = new AtomicInteger();
        assertFalse(CriticalDecisionEngine.rollChance(
                0.0f, () -> { calls.incrementAndGet(); return 0.0f; }).sampled());
        assertEquals(0, calls.get());
        assertTrue(CriticalDecisionEngine.rollChance(
                1.0f, () -> { calls.incrementAndGet(); return 0.75f; }).critical());
        assertEquals(1, calls.get());
        assertFalse(CriticalDecisionEngine.rollChance(
                0.5f, () -> { calls.incrementAndGet(); return 0.75f; }).critical());
        assertEquals(2, calls.get());
        assertFalse(CriticalDecisionEngine.rollChance(
                Float.NaN, () -> { calls.incrementAndGet(); return 0.0f; }).sampled());
        assertEquals(2, calls.get());
    }

    @Test
    void testSamplerScopeRestoresOnExceptionAndIsThreadLocal() throws Exception {
        assertFalse(CriticalDecisionEngine.hasTestSamplerForTesting());
        assertThrows(IllegalStateException.class, () -> {
            try (CriticalDecisionEngine.SamplerScope ignored =
                         CriticalDecisionEngine.useSamplerForTesting(player -> 0.25f)) {
                assertTrue(CriticalDecisionEngine.hasTestSamplerForTesting());
                assertThrows(IllegalStateException.class, () ->
                        CriticalDecisionEngine.useSamplerForTesting(player -> 0.5f));
                throw new IllegalStateException("intentional fixture failure");
            }
        });
        assertFalse(CriticalDecisionEngine.hasTestSamplerForTesting());

        try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            assertFalse(executor.submit(
                    CriticalDecisionEngine::hasTestSamplerForTesting).get());
        }
    }

    private static CriticalDecisionContribution contribution(
            String path, int priority, CriticalDecision decision) {
        return new CriticalDecisionContribution(
                Identifier.fromNamespaceAndPath("test", path), priority, decision);
    }
}
