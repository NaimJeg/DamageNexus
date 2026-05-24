package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.display.DamageContributionDescriptor;
import io.github.naimjeg.damagenexus.api.display.DamageContributionSummary;
import io.github.naimjeg.damagenexus.core.contribution.DamageContributionSummarizer;

import java.util.List;

public interface CombatContributionLog {

    void summary(
            int appliedCount,
            int rejectedCount,
            int noOpCount
    );

    default void noOpSummary(DamageContributionSummary summary) {
    }

    default void noOp(DamageContributionDescriptor descriptor) {
    }

    void applied(DamageContributionDescriptor descriptor);

    void rejected(DamageContributionDescriptor descriptor);

    default void appliedSummary(DamageContributionSummary summary) {
    }

    default void rejectedSummary(DamageContributionSummary summary) {
    }

    default void emit(
            List<DamageContributionDescriptor> applied,
            List<DamageContributionDescriptor> rejected,
            List<DamageContributionDescriptor> noOps
    ) {
        int appliedCount = applied == null ? 0 : applied.size();
        int rejectedCount = rejected == null ? 0 : rejected.size();
        int noOpCount = noOps == null ? 0 : noOps.size();

        if (appliedCount == 0 && rejectedCount == 0 && noOpCount == 0) {
            return;
        }

        summary(appliedCount, rejectedCount, noOpCount);

        for (DamageContributionSummary summary
                : DamageContributionSummarizer.summarize(applied)) {
            appliedSummary(summary);
        }

        for (DamageContributionSummary summary
                : DamageContributionSummarizer.summarize(rejected)) {
            rejectedSummary(summary);
        }

        for (DamageContributionSummary summary
                : DamageContributionSummarizer.summarize(noOps)) {
            noOpSummary(summary);
        }

        if (applied != null) {
            for (DamageContributionDescriptor descriptor : applied) {
                applied(descriptor);
            }
        }

        if (rejected != null) {
            for (DamageContributionDescriptor descriptor : rejected) {
                rejected(descriptor);
            }
        }

        if (noOps != null) {
            for (DamageContributionDescriptor descriptor : noOps) {
                noOp(descriptor);
            }
        }
    }
}
