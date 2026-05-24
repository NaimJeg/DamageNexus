package io.github.naimjeg.damagenexus.core.contribution;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.display.DamageContributionDescriptor;
import io.github.naimjeg.damagenexus.api.display.DamageContributionStatus;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DamageContributionCollector {

    private final List<DamageContributionDescriptor> entries =
            new ArrayList<>();

    public void recordApplied(DamageContributionDescriptor descriptor) {
        recordWithStatus(
                descriptor,
                DamageContributionStatus.APPLIED
        );
    }

    public void recordRejected(DamageContributionDescriptor descriptor) {
        recordWithStatus(
                descriptor,
                DamageContributionStatus.REJECTED
        );
    }

    public void recordNoOp(DamageContributionDescriptor descriptor) {
        recordWithStatus(
                descriptor,
                DamageContributionStatus.NO_OP
        );
    }

    public void record(
            DamageMutationResult result,
            Supplier<DamageContributionDescriptor> descriptor
    ) {
        if (result == null || descriptor == null) {
            return;
        }

        DamageContributionDescriptor resolved = descriptor.get();

        if (resolved == null) {
            return;
        }

        recordWithStatus(
                resolved,
                statusFromResult(result)
        );
    }

    public List<DamageContributionDescriptor> entries() {
        return List.copyOf(entries);
    }

    public List<DamageContributionDescriptor> applied() {
        return filtered(DamageContributionStatus.APPLIED);
    }

    public List<DamageContributionDescriptor> rejected() {
        return filtered(DamageContributionStatus.REJECTED);
    }

    public List<DamageContributionDescriptor> noOps() {
        return filtered(DamageContributionStatus.NO_OP);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @ApiStatus.Internal
    public int checkpoint() {
        return entries.size();
    }

    @ApiStatus.Internal
    public void rollback(int checkpoint) {
        if (checkpoint < 0 || checkpoint > entries.size()) {
            throw new IllegalArgumentException(
                    "Invalid contribution checkpoint: " + checkpoint
            );
        }
        entries.subList(checkpoint, entries.size()).clear();
    }

    private void recordWithStatus(
            DamageContributionDescriptor descriptor,
            DamageContributionStatus status
    ) {
        if (descriptor == null) {
            return;
        }

        entries.add(descriptor.withStatus(status));
    }

    private List<DamageContributionDescriptor> filtered(
            DamageContributionStatus status
    ) {
        return entries.stream()
                .filter(entry -> entry.status() == status)
                .toList();
    }

    private static DamageContributionStatus statusFromResult(
            DamageMutationResult result
    ) {
        if (result.applied()) {
            return DamageContributionStatus.APPLIED;
        }

        if (result.rejected()) {
            return DamageContributionStatus.REJECTED;
        }

        return DamageContributionStatus.NO_OP;
    }
}
