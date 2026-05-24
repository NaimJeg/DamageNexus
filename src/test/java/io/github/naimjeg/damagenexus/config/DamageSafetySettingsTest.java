package io.github.naimjeg.damagenexus.config;

import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageSafetySettingsTest {

    @Test
    void defaultsAndConfigSpecBoundsAreStable() {
        DamageSafetySettings defaults = DamageSafetySettings.defaults();
        assertEquals(5, defaults.maxRecursionDepth());
        assertEquals(64, defaults.maxDerivedRequestsPerRoot());
        assertEquals(2048, defaults.maxManagedRequestsPerServerTick());

        assertNotNull(DamageNexusConfig.SPEC);
        assertEquals(
                5,
                DamageSafetyConfigSpec.MAX_RECURSION_DEPTH.getDefault()
        );
        assertEquals(
                64,
                DamageSafetyConfigSpec.MAX_DERIVED_REQUESTS_PER_ROOT
                        .getDefault()
        );
        assertEquals(
                2048,
                DamageSafetyConfigSpec.MAX_MANAGED_REQUESTS_PER_SERVER_TICK
                        .getDefault()
        );
    }

    @Test
    void zeroNegativeAndHardLimitOverflowAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DamageSafetySettings(0, 1, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new DamageSafetySettings(1, -1, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new DamageSafetySettings(
                        1,
                        1,
                        DamageSafetySettings
                                .HARD_MAX_MANAGED_REQUESTS_PER_SERVER_TICK + 1
                )
        );
    }

    @Test
    void settingsObserveReloadedSafetyImmediately() throws Exception {
        DamageNexusConfigValues original = DamageNexusConfig.current();
        DamageSafetySettings replacement =
                new DamageSafetySettings(2, 3, 4);
        DamageNexusConfigValues values = new DamageNexusConfigValues(
                original.developer(),
                original.diagnostics(),
                original.tooltips(),
                original.formulas(),
                original.vanillaCompatibility(),
                replacement
        );

        Field current = DamageNexusConfig.class.getDeclaredField("CURRENT");
        current.setAccessible(true);
        try {
            current.set(null, values);
            assertEquals(2, DamageNexusSettings.maxRecursionDepth());
            assertEquals(3, DamageNexusSettings.maxDerivedRequestsPerRoot());
            assertEquals(
                    4,
                    DamageNexusSettings.maxManagedRequestsPerServerTick()
            );
        } finally {
            current.set(null, original);
        }
    }
}
