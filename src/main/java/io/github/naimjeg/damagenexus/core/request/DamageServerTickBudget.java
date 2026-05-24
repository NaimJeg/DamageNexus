package io.github.naimjeg.damagenexus.core.request;

import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.api.damage.DamageLineage;
import io.github.naimjeg.damagenexus.config.DamageSafetySettings;

/** Per-MinecraftServer state; reset lazily from the authoritative tick index. */
final class DamageServerTickBudget {

    private int tickIndex = Integer.MIN_VALUE;
    private int managedRequests;

    synchronized DamageAdmissionResult tryAdmit(
            DamageLineage lineage,
            int currentTick,
            DamageSafetySettings settings
    ) {
        advanceTick(currentTick);

        int rootCount = lineage.derivedRequestCountInternal();
        if (managedRequests
                >= settings.maxManagedRequestsPerServerTick()) {
            return DamageAdmissionResult.rejected(
                    DamageFailureReason.SERVER_TICK_BUDGET_EXHAUSTED,
                    rootCount,
                    managedRequests
            );
        }

        if (lineage.hasParent()
                && !lineage.reserveDerivedRequestInternal(
                        settings.maxDerivedRequestsPerRoot()
                )) {
            return DamageAdmissionResult.rejected(
                    DamageFailureReason.ROOT_DERIVATION_LIMIT,
                    lineage.derivedRequestCountInternal(),
                    managedRequests
            );
        }

        managedRequests++;
        return DamageAdmissionResult.admitted(
                lineage.derivedRequestCountInternal(),
                managedRequests
        );
    }

    synchronized int count(int currentTick) {
        advanceTick(currentTick);
        return managedRequests;
    }

    private void advanceTick(int currentTick) {
        if (tickIndex != currentTick) {
            tickIndex = currentTick;
            managedRequests = 0;
        }
    }
}
