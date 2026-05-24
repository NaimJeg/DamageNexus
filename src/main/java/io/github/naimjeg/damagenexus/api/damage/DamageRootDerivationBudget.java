package io.github.naimjeg.damagenexus.api.damage;

import java.util.concurrent.atomic.AtomicInteger;

/** Root-owned counter shared by every lineage branch and released with it. */
final class DamageRootDerivationBudget {

    private final AtomicInteger derivedRequests = new AtomicInteger();

    boolean tryReserve(int maximum) {
        if (maximum <= 0) {
            throw new IllegalArgumentException(
                    "Maximum derived requests must be positive"
            );
        }

        while (true) {
            int current = derivedRequests.get();
            if (current >= maximum) {
                return false;
            }
            if (derivedRequests.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    int count() {
        return derivedRequests.get();
    }
}
