package io.github.naimjeg.damagenexus.api.display;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record DamageContributionSummary(
        DamageContributionSourceKind sourceKind,
        DamageContributionOperationKind operationKind,
        DamagePhase phase,

        Optional<Identifier> channel,
        Optional<DamageApplicationBucket> applicationBucket,
        Optional<Identifier> preMultiplierBucket,

        Optional<Identifier> displayGroup,
        Optional<Identifier> displaySubgroup,
        Optional<String> displayName,
        Optional<String> traceLabel,

        int count,
        float totalValue,
        DamageContributionStatus status,

        List<DamageContributionDescriptor> entries
) {
    public DamageContributionSummary {
        sourceKind = sourceKind == null
                ? DamageContributionSourceKind.UNKNOWN
                : sourceKind;

        if (operationKind == null) {
            operationKind = DamageContributionOperationKind.UNKNOWN;
        }

        if (phase == null) {
            throw new IllegalArgumentException("Contribution summary phase cannot be null");
        }

        if (status == null) {
            status = DamageContributionStatus.APPLIED;
        }

        channel = channel == null ? Optional.empty() : channel;
        applicationBucket = applicationBucket == null ? Optional.empty() : applicationBucket;
        preMultiplierBucket = preMultiplierBucket == null ? Optional.empty() : preMultiplierBucket;
        displayGroup = displayGroup == null ? Optional.empty() : displayGroup;
        displaySubgroup = displaySubgroup == null ? Optional.empty() : displaySubgroup;
        displayName = displayName == null ? Optional.empty() : displayName;
        traceLabel = traceLabel == null ? Optional.empty() : traceLabel;

        count = Math.max(0, count);

        if (!Float.isFinite(totalValue)) {
            totalValue = 0.0f;
        }

        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
