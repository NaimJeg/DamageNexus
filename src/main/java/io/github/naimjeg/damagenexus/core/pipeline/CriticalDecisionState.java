package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.critical.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Mutable transaction-local decision state; never shared with child requests. */
final class CriticalDecisionState {
    private static final Comparator<CriticalDecisionContribution> ORDER =
            Comparator.comparingInt(CriticalDecisionContribution::priority).reversed()
                    .thenComparing(c -> c.sourceId().toString())
                    .thenComparing(c -> c.decision().serializedName());
    private final ArrayList<CriticalDecisionContribution> contributions = new ArrayList<>();
    private boolean providerCollectionClaimed;
    private boolean frozen;
    private CriticalDecision effectiveDecision = CriticalDecision.DEFAULT;
    private boolean critical;
    private CriticalDecisionOutcome outcome = CriticalDecisionOutcome.UNRESOLVED;
    private boolean chanceSampled;
    private boolean effectApplied;

    void add(CriticalDecisionContribution contribution) {
        if (frozen) throw new IllegalStateException("Critical decision is frozen");
        contributions.add(Objects.requireNonNull(contribution, "contribution"));
    }

    boolean claimProviderCollection() {
        if (providerCollectionClaimed) return false;
        providerCollectionClaimed = true;
        return true;
    }

    CriticalDecision resolveContributions() {
        if (contributions.isEmpty()) return CriticalDecision.DEFAULT;
        int highest = contributions.stream().mapToInt(CriticalDecisionContribution::priority).max().orElseThrow();
        return contributions.stream()
                .filter(c -> c.priority() == highest)
                .anyMatch(c -> c.decision() == CriticalDecision.SUPPRESS_CRITICAL)
                ? CriticalDecision.SUPPRESS_CRITICAL
                : CriticalDecision.FORCE_CRITICAL;
    }

    void freeze(CriticalDecision decision, boolean finalCritical,
                CriticalDecisionOutcome finalOutcome, boolean sampled) {
        if (frozen) throw new IllegalStateException("Critical decision already frozen");
        CriticalDecision checkedDecision =
                Objects.requireNonNull(decision, "decision");
        CriticalDecisionOutcome checkedOutcome =
                Objects.requireNonNull(finalOutcome, "outcome");
        new CriticalDecisionSnapshot(
                true,
                checkedDecision,
                finalCritical,
                checkedOutcome,
                sampled,
                false,
                orderedContributions()
        );
        effectiveDecision = checkedDecision;
        critical = finalCritical;
        outcome = checkedOutcome;
        chanceSampled = sampled;
        frozen = true;
    }

    void markEffectApplied() {
        if (!frozen) throw new IllegalStateException("Critical decision is not frozen");
        if (!critical) {
            throw new IllegalStateException(
                    "Critical effect requires a critical decision result");
        }
        if (effectApplied) throw new IllegalStateException("Critical effect already applied");
        effectApplied = true;
    }

    boolean frozen() { return frozen; }
    boolean critical() { return critical; }
    boolean effectApplied() { return effectApplied; }

    CriticalDecisionSnapshot snapshot() {
        if (!frozen) {
            return CriticalDecisionSnapshot.unresolved();
        }
        List<CriticalDecisionContribution> ordered = contributions.stream().sorted(ORDER).toList();
        return new CriticalDecisionSnapshot(
                frozen, effectiveDecision, critical, outcome,
                chanceSampled, effectApplied, ordered
        );
    }

    List<CriticalDecisionContribution> orderedContributions() {
        return contributions.stream().sorted(ORDER).toList();
    }

    Checkpoint checkpoint() {
        return new Checkpoint(List.copyOf(contributions), providerCollectionClaimed,
                frozen, effectiveDecision,
                critical, outcome, chanceSampled, effectApplied);
    }

    void restore(Checkpoint checkpoint) {
        contributions.clear();
        contributions.addAll(checkpoint.contributions());
        providerCollectionClaimed = checkpoint.providerCollectionClaimed();
        frozen = checkpoint.frozen();
        effectiveDecision = checkpoint.effectiveDecision();
        critical = checkpoint.critical();
        outcome = checkpoint.outcome();
        chanceSampled = checkpoint.chanceSampled();
        effectApplied = checkpoint.effectApplied();
    }

    record Checkpoint(
            List<CriticalDecisionContribution> contributions,
            boolean providerCollectionClaimed,
            boolean frozen,
            CriticalDecision effectiveDecision,
            boolean critical,
            CriticalDecisionOutcome outcome,
            boolean chanceSampled,
            boolean effectApplied
    ) { }
}
