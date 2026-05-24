package io.github.naimjeg.damagenexus.api.event;

/**
 * Server-side completed-damage callback registered during
 * {@link DamageNexusRegisterEvent}.
 *
 * <p>Unlike the observational NeoForge {@link DamageSettledEvent}, this
 * callback runs inside a framework-controlled dynamic scope and may obtain
 * temporary child-request authority from its argument. Ordinary callback
 * failures are isolated per registered listener after the parent damage has
 * committed.</p>
 */
@FunctionalInterface
public interface DamageSettlementListener {

    void onDamageSettled(DamageSettlementCallback callback);
}
