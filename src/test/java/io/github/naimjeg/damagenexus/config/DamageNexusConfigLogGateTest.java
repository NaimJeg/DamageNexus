package io.github.naimjeg.damagenexus.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNexusConfigLogGateTest {

    @AfterEach
    void reset() {
        DamageNexusConfig.resetConfigLogGateForTesting();
    }

    @Test
    void identicalEffectiveConfigLogsOnceAndChangedConfigLogsAgain() {
        DamageNexusConfigValues off = DamageNexusConfigValues.defaults();
        DamageNexusConfigValues summary = new DamageNexusConfigValues(
                off.developer(),
                new DiagnosticsSettings(DiagnosticMode.SUMMARY),
                off.tooltips(),
                off.formulas(),
                off.vanillaCompatibility(),
                off.safety()
        );

        assertTrue(DamageNexusConfig.shouldLogBakedConfig(off));
        assertFalse(DamageNexusConfig.shouldLogBakedConfig(off));
        assertTrue(DamageNexusConfig.shouldLogBakedConfig(summary));
        assertFalse(DamageNexusConfig.shouldLogBakedConfig(summary));
    }
}
