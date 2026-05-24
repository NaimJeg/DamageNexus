package io.github.naimjeg.damagenexus.core.config;

import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.config.DamageNexusConfigValues;
import io.github.naimjeg.damagenexus.config.DiagnosticMode;
import io.github.naimjeg.damagenexus.config.DamageSafetySettings;
import io.github.naimjeg.damagenexus.config.VanillaReductionCompatibilityMode;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLogKind;

public final class DamageNexusSettings {

    private DamageNexusSettings() {
    }

    public static DamageNexusConfigValues current() {
        return DamageNexusConfig.current();
    }

    public static boolean testCommandsEnabled() {
        return current().developer().testCommandsEnabled();
    }

    public static boolean strictProcessorErrors() {
        return current().developer().strictProcessorErrors();
    }

    public static boolean strictRuleErrors() {
        return current().developer().strictRuleErrors();
    }

    public static VanillaReductionCompatibilityMode vanillaReductionMode() {
        return current().vanillaCompatibility().mode();
    }

    public static boolean suppressVanillaArmorReduction() {
        return current().vanillaCompatibility().shouldSuppressArmor();
    }

    public static boolean suppressVanillaEnchantmentReduction() {
        return current().vanillaCompatibility().shouldSuppressEnchantments();
    }

    public static boolean suppressVanillaMobEffectReduction() {
        return current().vanillaCompatibility().shouldSuppressMobEffects();
    }

    public static boolean suppressVanillaInnateResistanceReduction() {
        return current().vanillaCompatibility().shouldSuppressInnateResistance();
    }

    public static DiagnosticMode diagnosticMode() {
        return current().diagnostics().diagnosticMode();
    }

    public static boolean compatibilityDiagnosticsEnabled() {
        return current().diagnostics().compatibilityDiagnosticsEnabled();
    }

    public static boolean summaryTraceEnabled() {
        return current().diagnostics().summaryTraceEnabled();
    }

    public static boolean fullTraceEnabled() {
        return current().diagnostics().fullTraceEnabled();
    }

    public static boolean transactionTrackingEnabled() {
        return current().diagnostics().transactionTrackingEnabled();
    }

    public static DamageSafetySettings safety() {
        return current().safety();
    }

    public static int maxRecursionDepth() {
        return safety().maxRecursionDepth();
    }

    public static int maxDerivedRequestsPerRoot() {
        return safety().maxDerivedRequestsPerRoot();
    }

    public static int maxManagedRequestsPerServerTick() {
        return safety().maxManagedRequestsPerServerTick();
    }

    public static boolean shouldEmitServer(DamageNexusLogKind kind) {
        return current().diagnostics().shouldEmitServer(kind);
    }

}
