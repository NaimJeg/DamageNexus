package io.github.naimjeg.damagenexus.core.contribution;

import io.github.naimjeg.damagenexus.api.display.*;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class DamageContributionSummarizer {

    private DamageContributionSummarizer() {
    }

    public static List<DamageContributionSummary> summarize(
            List<DamageContributionDescriptor> descriptors
    ) {
        if (descriptors == null || descriptors.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<Key, MutableSummary> grouped =
                new LinkedHashMap<>();

        for (DamageContributionDescriptor descriptor : descriptors) {
            if (descriptor == null) {
                continue;
            }

            Key key = Key.from(descriptor);

            grouped.computeIfAbsent(
                    key,
                    ignored -> new MutableSummary(descriptor)
            ).add(descriptor);
        }

        List<DamageContributionSummary> out =
                new ArrayList<>(grouped.size());

        for (MutableSummary summary : grouped.values()) {
            out.add(summary.toSummary());
        }

        return List.copyOf(out);
    }

    private record Key(
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
            DamageContributionStatus status
    ) {
        static Key from(DamageContributionDescriptor descriptor) {
            return new Key(
                    descriptor.sourceKind(),
                    descriptor.operationKind(),
                    descriptor.phase(),
                    descriptor.channel(),
                    descriptor.applicationBucket(),
                    descriptor.preMultiplierBucket(),
                    descriptor.displayGroup(),
                    descriptor.displaySubgroup(),
                    descriptor.displayName(),
                    descriptor.traceLabel(),
                    descriptor.status()
            );
        }
    }

    private static final class MutableSummary {
        private final Key key;
        private final List<DamageContributionDescriptor> entries =
                new ArrayList<>();

        private float totalValue = 0.0f;

        private MutableSummary(DamageContributionDescriptor first) {
            this.key = Key.from(first);
        }

        private void add(DamageContributionDescriptor descriptor) {
            entries.add(descriptor);

            float value = descriptor.value();

            if (Float.isFinite(value)) {
                totalValue += value;
            }
        }

        private DamageContributionSummary toSummary() {
            return new DamageContributionSummary(
                    key.sourceKind(),
                    key.operationKind(),
                    key.phase(),
                    key.channel(),
                    key.applicationBucket(),
                    key.preMultiplierBucket(),
                    key.displayGroup(),
                    key.displaySubgroup(),
                    key.displayName(),
                    key.traceLabel(),
                    entries.size(),
                    totalValue,
                    key.status(),
                    entries
            );
        }
    }
}
