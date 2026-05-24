package io.github.naimjeg.damagenexus.api.display;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record DamageContributionDescriptor(
        Identifier id,
        DamageContributionSourceKind sourceKind,
        DamageContributionOperationKind operationKind,
        DamagePhase phase,

        Optional<Identifier> channel,
        Optional<DamageApplicationBucket> applicationBucket,
        Optional<Identifier> preMultiplierBucket,

        float value,

        DamageContributionStatus status,

        Optional<Identifier> displayGroup,
        Optional<Identifier> displaySubgroup,
        Optional<String> displayName,
        List<String> tooltipLines,

        Optional<String> traceLabel,
        boolean runtimeOnly,
        boolean debugOnly
) {
    public DamageContributionDescriptor {
        if (id == null) {
            throw new IllegalArgumentException("Damage contribution id cannot be null");
        }

        if (sourceKind == null) {
            sourceKind = DamageContributionSourceKind.UNKNOWN;
        }

        if (operationKind == null) {
            throw new IllegalArgumentException("Damage contribution operation kind cannot be null: " + id);
        }

        if (phase == null) {
            throw new IllegalArgumentException("Damage contribution phase cannot be null: " + id);
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
        tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
        traceLabel = traceLabel == null ? Optional.empty() : traceLabel;
    }

    public DamageContributionDescriptor withStatus(
            DamageContributionStatus status
    ) {
        return new DamageContributionDescriptor(
                id,
                sourceKind,
                operationKind,
                phase,
                channel,
                applicationBucket,
                preMultiplierBucket,
                value,
                status,
                displayGroup,
                displaySubgroup,
                displayName,
                tooltipLines,
                traceLabel,
                runtimeOnly,
                debugOnly
        );
    }
}