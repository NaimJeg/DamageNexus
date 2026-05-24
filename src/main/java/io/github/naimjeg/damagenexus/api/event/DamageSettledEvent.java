package io.github.naimjeg.damagenexus.api.event;

import io.github.naimjeg.damagenexus.api.damage.DamageSettlementSnapshot;
import net.neoforged.bus.api.Event;

import java.util.Objects;

/**
 * Synchronous, non-cancelable server event posted after a managed damage
 * transaction and all mutable DamageNexus activity scopes have been removed.
 * Official events are drained through a non-recursive FIFO. This NeoForge
 * event is observational: derived-request authority is available only from a
 * registered {@link DamageSettlementListener} invocation. Re-posting the same
 * event object remains observational. If a NeoForge listener throws, the bus
 * may skip listeners not yet invoked; the publisher preserves the committed
 * settlement and continues framework-owned callback/FIFO cleanup.
 */
public final class DamageSettledEvent extends Event {

    private final DamageSettlementSnapshot snapshot;
    /**
     * Creates an observational event with no child authority. Posting an event
     * created through this constructor does not make it an official framework
     * completion.
     */
    public DamageSettledEvent(DamageSettlementSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public DamageSettlementSnapshot snapshot() {
        return snapshot;
    }
}
