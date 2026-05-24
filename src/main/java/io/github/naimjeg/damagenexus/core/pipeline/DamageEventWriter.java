package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.core.trace.DamageNexusTransaction;
import io.github.naimjeg.damagenexus.core.trace.DamageNexusTransactionTracker;
import io.github.naimjeg.damagenexus.diagnostics.logging.CombatTrace;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

final class DamageEventWriter {

    private final DamageEventSnapshot event;
    private final DamageDiagnosticsContext diagnostics;
    private final CombatTrace trace;

    DamageEventWriter(
            DamageEventSnapshot event,
            DamageDiagnosticsContext diagnostics,
            CombatTrace trace
    ) {
        this.event = event;
        this.diagnostics = diagnostics;
        this.trace = trace;
    }

    void applyIncomingDamage(DamagePipelineResult result) {
        float eventAmountBeforeSet =
                event.neoforgeEvent().getAmount();

        event.neoforgeEvent().setAmount(
                result.finalEventDamage()
        );

        float eventAmountAfterSet =
                event.neoforgeEvent().getAmount();

        trace.transaction().apply(
                event.eventOriginalAmount(),
                event.initialBaseAmount(),
                result.offensiveTotal(),
                result.finalEventDamage()
        );

        recordIncomingCandidate(
                result,
                eventAmountBeforeSet,
                eventAmountAfterSet
        );

        trace.calculation().defensiveSummary(
                result.finalEventDamage()
        );

        suppressVanillaReductions();
    }

    private void recordIncomingCandidate(
            DamagePipelineResult result,
            float eventAmountBeforeSet,
            float eventAmountAfterSet
    ) {
        if (!diagnostics.transactionEnabled()) {
            return;
        }

        DamageNexusTransaction tx =
                diagnostics.createIncomingTransaction(
                        event,
                        result,
                        eventAmountBeforeSet,
                        eventAmountAfterSet
                );

        if (tx == null) {
            return;
        }

        DamageNexusTransactionTracker.recordIncomingCandidate(
                event.neoforgeEvent().getContainer(),
                tx
        );
    }

    private void suppressVanillaReductions() {
        switch (DamageNexusSettings.vanillaReductionMode()) {
            case FULL_REPLACEMENT -> suppressAllVanillaReductions();
            case CONFIGURABLE -> suppressConfiguredVanillaReductions();
            case COOPERATIVE -> {
                // Intentionally do not suppress vanilla reductions.
            }
        }
    }

    private void suppressAllVanillaReductions() {
        suppressVanillaReduction(DamageContainer.Reduction.ARMOR);
        suppressVanillaReduction(DamageContainer.Reduction.ENCHANTMENTS);
        suppressVanillaReduction(DamageContainer.Reduction.MOB_EFFECTS);
        suppressVanillaReduction(DamageContainer.Reduction.INNATE_RESISTANCE);
    }

    private void suppressConfiguredVanillaReductions() {
        if (DamageNexusSettings.suppressVanillaArmorReduction()) {
            suppressVanillaReduction(DamageContainer.Reduction.ARMOR);
        }

        if (DamageNexusSettings.suppressVanillaEnchantmentReduction()) {
            suppressVanillaReduction(DamageContainer.Reduction.ENCHANTMENTS);
        }

        if (DamageNexusSettings.suppressVanillaMobEffectReduction()) {
            suppressVanillaReduction(DamageContainer.Reduction.MOB_EFFECTS);
        }

        if (DamageNexusSettings.suppressVanillaInnateResistanceReduction()) {
            suppressVanillaReduction(DamageContainer.Reduction.INNATE_RESISTANCE);
        }
    }

    private void suppressVanillaReduction(
            DamageContainer.Reduction reduction
    ) {
        event.neoforgeEvent().addReductionModifier(
                reduction,
                (container, vanillaReduction) -> 0.0f
        );
    }

}
