package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.core.trace.DamageNexusTransaction;
import io.github.naimjeg.damagenexus.core.trace.DamageNexusTransactionTracker;
import io.github.naimjeg.damagenexus.core.trace.DamageObservationSnapshot;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.diagnostics.logging.CombatTrace;
import io.github.naimjeg.damagenexus.diagnostics.logging.CombatTraceFactory;
import net.minecraft.world.entity.LivingEntity;

public final class DamageDiagnosticsContext {

    private final long damageId;
    private final CombatTrace trace;
    private final DamageObservationSnapshot incomingObservation;

    private DamageDiagnosticsContext(
            long damageId,
            CombatTrace trace,
            DamageObservationSnapshot incomingObservation
    ) {
        this.damageId = damageId;
        this.trace = trace;
        this.incomingObservation = incomingObservation;
    }

    public static DamageDiagnosticsContext create(
            long damageId,
            LivingEntity victim
    ) {
        DamageObservationSnapshot observation =
                victim != null && DamageNexusTransactionTracker.enabled()
                        ? DamageObservationSnapshot.capture(victim)
                        : null;

        return new DamageDiagnosticsContext(
                damageId,
                CombatTraceFactory.create(damageId),
                observation
        );
    }

    public long damageId() {
        return damageId;
    }

    public CombatTrace trace() {
        return trace;
    }

    public boolean transactionEnabled() {
        return incomingObservation != null;
    }

    DamageNexusTransaction createIncomingTransaction(
            DamageEventSnapshot event,
            DamagePipelineResult result,
            float eventAmountBeforeSet,
            float eventAmountAfterSet
    ) {
        if (incomingObservation == null) {
            return null;
        }

        return new DamageNexusTransaction(
                damageId,
                event.attacker(),
                event.victim(),
                event.source(),

                event.eventOriginalAmount(),
                event.initialBaseAmount(),
                result.offensiveTotal(),
                result.finalEventDamage(),

                eventAmountBeforeSet,
                eventAmountAfterSet,

                Float.NaN,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                false,

                incomingObservation.health(),
                incomingObservation.absorption(),
                incomingObservation.invulnerableTime(),
                incomingObservation.gameTime(),

                DamageNexusSettings.vanillaReductionMode(),
                DamageNexusSettings.suppressVanillaArmorReduction(),
                DamageNexusSettings.suppressVanillaEnchantmentReduction(),
                DamageNexusSettings.suppressVanillaMobEffectReduction(),
                DamageNexusSettings.suppressVanillaInnateResistanceReduction()
        );
    }
}
