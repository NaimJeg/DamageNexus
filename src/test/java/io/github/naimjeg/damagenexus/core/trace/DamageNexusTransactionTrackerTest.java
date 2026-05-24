package io.github.naimjeg.damagenexus.core.trace;

import io.github.naimjeg.damagenexus.config.VanillaReductionCompatibilityMode;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNexusTransactionTrackerTest {

    private static final Identifier SOURCE =
            Identifier.fromNamespaceAndPath("test", "source");
    private static final Identifier OTHER_SOURCE =
            Identifier.fromNamespaceAndPath("test", "other_source");

    @Test
    void exactMatchRemovesOnlyMatchedTransaction() {
        DamageNexusTransaction unrelated = tx(1, 4.0f, 100);
        DamageNexusTransaction exact = tx(2, 6.0f, 100);
        Deque<DamageNexusTransaction> queue =
                new ArrayDeque<>(List.of(unrelated, exact));

        DamageNexusTransaction result = poll(
                queue,
                SOURCE,
                101,
                6.0f,
                Map.of(unrelated.damageId(), OTHER_SOURCE)
        );

        assertSame(exact, result);
        assertEquals(List.of(unrelated), new ArrayList<>(queue));
    }

    @Test
    void nestedSameVictimSourceAndAmountUsesLifoWithoutDeletingOuter() {
        DamageNexusTransaction outer = tx(1, 6.0f, 100);
        DamageNexusTransaction inner = tx(2, 6.0f, 100);
        Deque<DamageNexusTransaction> queue =
                new ArrayDeque<>(List.of(outer, inner));

        assertSame(inner, poll(queue, SOURCE, 100, 6.0f));
        assertEquals(List.of(outer), new ArrayList<>(queue));
        assertSame(outer, poll(queue, SOURCE, 100, 6.0f));
        assertTrue(queue.isEmpty());
    }

    @Test
    void singleRecentSourceCandidateMayUseExplicitHeuristicFallback() {
        DamageNexusTransaction candidate = tx(1, 10.0f, 100);
        Deque<DamageNexusTransaction> queue =
                new ArrayDeque<>(List.of(candidate));

        assertSame(candidate, poll(queue, SOURCE, 101, 7.0f));
        assertTrue(queue.isEmpty());
    }

    @Test
    void ambiguousHeuristicFallbackFailsWithoutRemovingCandidates() {
        DamageNexusTransaction outer = tx(1, 10.0f, 100);
        DamageNexusTransaction inner = tx(2, 9.0f, 100);
        Deque<DamageNexusTransaction> queue =
                new ArrayDeque<>(List.of(outer, inner));

        assertNull(poll(queue, SOURCE, 101, 7.0f));
        assertEquals(List.of(outer, inner), new ArrayList<>(queue));
    }

    @Test
    void expiredEntriesAreRemovedWithoutDiscardingFreshCandidate() {
        DamageNexusTransaction expired = tx(1, 8.0f, 0);
        DamageNexusTransaction fresh = tx(2, 6.0f, 100);
        Deque<DamageNexusTransaction> queue =
                new ArrayDeque<>(List.of(expired, fresh));

        assertSame(fresh, poll(queue, SOURCE, 101, 6.0f));
        assertTrue(queue.isEmpty());
    }

    @Test
    void queueCapacityEvictsOldestAndRemainsBounded() {
        Deque<DamageNexusTransaction> queue = new ArrayDeque<>();
        for (int id = 0; id < 70; id++) {
            queue.addLast(tx(id, 1.0f, 100));
        }

        List<DamageNexusTransaction> dropped =
                DamageNexusTransactionTracker.trimToCapacity(queue);

        assertEquals(6, dropped.size());
        assertEquals(64, queue.size());
        assertEquals(0, dropped.getFirst().damageId());
        assertEquals(6, queue.getFirst().damageId());
    }

    private static DamageNexusTransaction poll(
            Deque<DamageNexusTransaction> queue,
            Identifier wantedSource,
            long now,
            float eventDamage
    ) {
        return poll(queue, wantedSource, now, eventDamage, Map.of());
    }

    private static DamageNexusTransaction poll(
            Deque<DamageNexusTransaction> queue,
            Identifier wantedSource,
            long now,
            float eventDamage,
            Map<Long, Identifier> overrides
    ) {
        Function<DamageNexusTransaction, Identifier> sourceResolver =
                tx -> overrides.getOrDefault(tx.damageId(), SOURCE);
        return DamageNexusTransactionTracker.pollMatchingPostTrackable(
                queue,
                wantedSource,
                now,
                sourceResolver,
                null,
                eventDamage
        );
    }

    static DamageNexusTransaction tx(long id, float preDamage, long gameTime) {
        return new DamageNexusTransaction(
                id,
                null,
                null,
                null,
                preDamage,
                preDamage,
                preDamage,
                preDamage,
                preDamage,
                preDamage,
                preDamage,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                false,
                20.0f,
                0.0f,
                0,
                gameTime,
                VanillaReductionCompatibilityMode.FULL_REPLACEMENT,
                true,
                true,
                true,
                true
        );
    }
}
