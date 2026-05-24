package io.github.naimjeg.damagenexus.core.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unforgeable framework capability used by registry mutation methods.
 *
 * <p>The constructor and lifecycle controls are package-private. Public
 * registry methods accept this type only so registries in other internal
 * packages can verify the same registration session; normal API consumers
 * never receive an instance.</p>
 */
public final class DamageNexusRegistrationAccess {

    private final Thread ownerThread;
    private final AtomicBoolean active = new AtomicBoolean(true);

    DamageNexusRegistrationAccess() {
        this.ownerThread = Thread.currentThread();
    }

    public void requireActive(String action) {
        if (!active.get()) {
            throw new IllegalStateException(
                    "DamageNexus registration capability has expired. action="
                            + action
            );
        }

        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException(
                    "DamageNexus registration must run synchronously on the "
                            + "registration event thread. action="
                            + action
            );
        }
    }

    void close() {
        active.set(false);
    }

    boolean isActive() {
        return active.get();
    }
}
