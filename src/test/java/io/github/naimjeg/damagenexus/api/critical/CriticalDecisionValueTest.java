package io.github.naimjeg.damagenexus.api.critical;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CriticalDecisionValueTest {
    @Test
    void stableCodecsRoundTripWithoutOrdinalsAndRejectUnknownValues() {
        for (CriticalDecision decision : CriticalDecision.values()) {
            JsonPrimitive encoded = CriticalDecision.CODEC.encodeStart(
                    JsonOps.INSTANCE, decision).getOrThrow().getAsJsonPrimitive();
            assertEquals(decision.serializedName(), encoded.getAsString());
            assertEquals(decision, CriticalDecision.CODEC.parse(
                    JsonOps.INSTANCE, encoded).getOrThrow());
        }
        for (CriticalDecisionOutcome outcome : CriticalDecisionOutcome.values()) {
            assertEquals(outcome, CriticalDecisionOutcome.CODEC.parse(
                    JsonOps.INSTANCE,
                    CriticalDecisionOutcome.CODEC.encodeStart(
                            JsonOps.INSTANCE, outcome).getOrThrow()).getOrThrow());
        }
        assertTrue(CriticalDecision.CODEC.parse(
                JsonOps.INSTANCE, new JsonPrimitive("future_mode")).error().isPresent());
    }

    @Test
    void snapshotSortsAndDefensivelyCopiesContributions() {
        ArrayList<CriticalDecisionContribution> mutable = new ArrayList<>();
        mutable.add(contribution("zmod", "low", 1, CriticalDecision.FORCE_CRITICAL));
        mutable.add(contribution("zmod", "same", 5, CriticalDecision.FORCE_CRITICAL));
        mutable.add(contribution("amod", "same", 5, CriticalDecision.SUPPRESS_CRITICAL));
        CriticalDecisionSnapshot snapshot = new CriticalDecisionSnapshot(
                true, CriticalDecision.SUPPRESS_CRITICAL, false,
                CriticalDecisionOutcome.SUPPRESSED, false, false, mutable);
        mutable.clear();
        assertEquals(List.of(
                        id("amod", "same"), id("zmod", "same"), id("zmod", "low")),
                snapshot.contributionSourceIds());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.contributions().clear());
        assertThrows(IllegalArgumentException.class,
                () -> contribution("test", "default", 0, CriticalDecision.DEFAULT));
    }

    @Test
    void snapshotRejectsEveryContradictoryPublicState() {
        CriticalDecisionContribution force = contribution(
                "test", "force", 10, CriticalDecision.FORCE_CRITICAL);
        CriticalDecisionContribution suppress = contribution(
                "test", "suppress", 10, CriticalDecision.SUPPRESS_CRITICAL);

        assertThrows(NullPointerException.class, () -> new CriticalDecisionSnapshot(
                false, CriticalDecision.DEFAULT, false,
                CriticalDecisionOutcome.UNRESOLVED, false, false, null));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                false, CriticalDecision.DEFAULT, false,
                CriticalDecisionOutcome.UNRESOLVED, false, false, List.of(force)));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.DEFAULT, false,
                CriticalDecisionOutcome.UNRESOLVED, false, false, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.DEFAULT, false,
                CriticalDecisionOutcome.FORCED, false, false, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.DEFAULT, true,
                CriticalDecisionOutcome.FORCED, false, false, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.DEFAULT, false,
                CriticalDecisionOutcome.SUPPRESSED, false, false, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.SUPPRESS_CRITICAL, false,
                CriticalDecisionOutcome.DEFAULT_NON_CRITICAL, false, false,
                List.of(suppress)));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.DEFAULT, false,
                CriticalDecisionOutcome.DEFAULT_NON_CRITICAL, false, true, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.DEFAULT, true,
                CriticalDecisionOutcome.ATTRIBUTE_CHANCE, false, true, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.FORCE_CRITICAL, true,
                CriticalDecisionOutcome.FORCED, true, true, List.of(force)));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.FORCE_CRITICAL, true,
                CriticalDecisionOutcome.FORCED, false, true,
                List.of(force, suppress)));
    }

    @Test
    void forceMayPreserveCapturedVanillaOutcomesAndDefaultOwnsChance() {
        CriticalDecisionContribution force = contribution(
                "test", "force", 10, CriticalDecision.FORCE_CRITICAL);
        assertDoesNotThrow(() -> new CriticalDecisionSnapshot(
                true, CriticalDecision.FORCE_CRITICAL, true,
                CriticalDecisionOutcome.FORCED, false, false, List.of(force)));
        assertDoesNotThrow(() -> new CriticalDecisionSnapshot(
                true, CriticalDecision.FORCE_CRITICAL, true,
                CriticalDecisionOutcome.VANILLA_MELEE, false, false, List.of(force)));
        assertDoesNotThrow(() -> new CriticalDecisionSnapshot(
                true, CriticalDecision.FORCE_CRITICAL, true,
                CriticalDecisionOutcome.VANILLA_PROJECTILE, false, false, List.of(force)));
        assertDoesNotThrow(() -> new CriticalDecisionSnapshot(
                true, CriticalDecision.DEFAULT, true,
                CriticalDecisionOutcome.ATTRIBUTE_CHANCE, true, false, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CriticalDecisionSnapshot(
                true, CriticalDecision.DEFAULT, true,
                CriticalDecisionOutcome.ATTRIBUTE_CHANCE, false, false, List.of()));
    }

    private static CriticalDecisionContribution contribution(
            String namespace, String path, int priority, CriticalDecision decision) {
        return new CriticalDecisionContribution(id(namespace, path), priority, decision);
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
