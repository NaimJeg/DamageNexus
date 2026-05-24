package io.github.naimjeg.damagenexus.config;

import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLogKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticsSettingsTest {

    @Test
    void diagnosticModesHaveExpectedServerOutputMatrix() {
        assertMode(DiagnosticMode.OFF, false, false, false);
        assertMode(DiagnosticMode.COMPATIBILITY, true, false, false);
        assertMode(DiagnosticMode.SUMMARY, true, true, false);
        assertMode(DiagnosticMode.FULL_TRACE, true, true, true);
    }

    @Test
    void defaultIsOffAndLogKindsContainOnlyDiagnosticDetailCategories() {
        assertEquals(
                DiagnosticMode.OFF,
                DiagnosticsSettings.defaults().diagnosticMode()
        );
        assertEquals(3, DamageNexusLogKind.values().length);
    }

    private static void assertMode(
            DiagnosticMode mode,
            boolean compatibility,
            boolean summary,
            boolean detail
    ) {
        DiagnosticsSettings settings = new DiagnosticsSettings(mode);
        assertEquals(compatibility, settings.shouldEmitServer(
                DamageNexusLogKind.COMPATIBILITY
        ));
        assertEquals(summary, settings.shouldEmitServer(
                DamageNexusLogKind.TRACE_SUMMARY
        ));
        assertEquals(detail, settings.shouldEmitServer(
                DamageNexusLogKind.TRACE_DETAIL
        ));
        assertEquals(compatibility, settings.compatibilityDiagnosticsEnabled());
        assertEquals(summary, settings.summaryTraceEnabled());
        assertEquals(detail, settings.fullTraceEnabled());
        if (mode == DiagnosticMode.OFF) {
            assertFalse(settings.transactionTrackingEnabled());
        } else {
            assertTrue(settings.transactionTrackingEnabled());
        }
    }
}
