package io.github.naimjeg.damagenexus.api.critical;

import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable public view of one transaction's critical-decision lifecycle. */
public record CriticalDecisionSnapshot(
        boolean frozen,
        CriticalDecision effectiveDecision,
        boolean critical,
        CriticalDecisionOutcome outcome,
        boolean chanceSampled,
        boolean effectApplied,
        List<CriticalDecisionContribution> contributions
) {
    private static final Comparator<CriticalDecisionContribution> ORDER =
            Comparator.comparingInt(CriticalDecisionContribution::priority)
                    .reversed()
                    .thenComparing(c -> c.sourceId().toString())
                    .thenComparing(c -> c.decision().serializedName());

    public CriticalDecisionSnapshot {
        Objects.requireNonNull(effectiveDecision, "effectiveDecision");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(contributions, "contributions");
        contributions = contributions.stream().sorted(ORDER).toList();

        if (!frozen) {
            if (effectiveDecision != CriticalDecision.DEFAULT
                    || critical || chanceSampled || effectApplied
                    || outcome != CriticalDecisionOutcome.UNRESOLVED
                    || !contributions.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unfrozen critical snapshot must equal the unresolved state");
            }
        } else if (outcome == CriticalDecisionOutcome.UNRESOLVED) {
            throw new IllegalArgumentException(
                    "Frozen critical snapshot requires a resolved outcome");
        } else {
            if (effectApplied && !critical) {
                throw new IllegalArgumentException(
                        "A critical effect cannot be applied to a non-critical result");
            }
            boolean criticalOutcome = switch (outcome) {
                case FORCED, VANILLA_MELEE, VANILLA_PROJECTILE, ATTRIBUTE_CHANCE -> true;
                default -> false;
            };
            if (critical != criticalOutcome) {
                throw new IllegalArgumentException(
                        "Critical flag does not match the resolved outcome");
            }
            if (chanceSampled && (effectiveDecision != CriticalDecision.DEFAULT
                    || (outcome != CriticalDecisionOutcome.ATTRIBUTE_CHANCE
                    && outcome != CriticalDecisionOutcome.DEFAULT_NON_CRITICAL))) {
                throw new IllegalArgumentException(
                        "Chance sampling is valid only for the default probability path");
            }
            if (outcome == CriticalDecisionOutcome.ATTRIBUTE_CHANCE
                    && !chanceSampled) {
                throw new IllegalArgumentException(
                        "Attribute chance outcome requires a recorded sample");
            }
            if (outcome == CriticalDecisionOutcome.ATTRIBUTE_CHANCE
                    && effectiveDecision != CriticalDecision.DEFAULT) {
                throw new IllegalArgumentException(
                        "Attribute chance outcome requires the default decision path");
            }
            if (outcome == CriticalDecisionOutcome.SUPPRESSED
                    && effectiveDecision != CriticalDecision.SUPPRESS_CRITICAL) {
                throw new IllegalArgumentException(
                        "Suppressed outcome requires a suppress decision");
            }
            if (outcome == CriticalDecisionOutcome.FORCED
                    && effectiveDecision != CriticalDecision.FORCE_CRITICAL) {
                throw new IllegalArgumentException(
                        "Forced outcome requires a force decision");
            }
            if (effectiveDecision == CriticalDecision.SUPPRESS_CRITICAL
                    && (outcome != CriticalDecisionOutcome.SUPPRESSED
                    || critical || chanceSampled || effectApplied)) {
                throw new IllegalArgumentException(
                        "Suppressed decision has inconsistent result state");
            }
            if (effectiveDecision == CriticalDecision.FORCE_CRITICAL
                    && (!critical || chanceSampled
                    || (outcome != CriticalDecisionOutcome.FORCED
                    && outcome != CriticalDecisionOutcome.VANILLA_MELEE
                    && outcome != CriticalDecisionOutcome.VANILLA_PROJECTILE))) {
                throw new IllegalArgumentException(
                        "Forced decision has inconsistent result state");
            }

            CriticalDecision expected = resolve(contributions);
            if (effectiveDecision != expected) {
                throw new IllegalArgumentException(
                        "Effective decision does not match highest-priority contributions");
            }
        }
    }

    public static CriticalDecisionSnapshot unresolved() {
        return new CriticalDecisionSnapshot(
                false, CriticalDecision.DEFAULT, false,
                CriticalDecisionOutcome.UNRESOLVED, false, false, List.of()
        );
    }

    public List<Identifier> contributionSourceIds() {
        return contributions.stream()
                .map(CriticalDecisionContribution::sourceId)
                .distinct()
                .toList();
    }

    private static CriticalDecision resolve(
            List<CriticalDecisionContribution> contributions
    ) {
        if (contributions.isEmpty()) {
            return CriticalDecision.DEFAULT;
        }
        int highestPriority = contributions.stream()
                .mapToInt(CriticalDecisionContribution::priority)
                .max()
                .orElseThrow();
        return contributions.stream()
                .filter(contribution -> contribution.priority() == highestPriority)
                .anyMatch(contribution -> contribution.decision()
                        == CriticalDecision.SUPPRESS_CRITICAL)
                ? CriticalDecision.SUPPRESS_CRITICAL
                : CriticalDecision.FORCE_CRITICAL;
    }
}
