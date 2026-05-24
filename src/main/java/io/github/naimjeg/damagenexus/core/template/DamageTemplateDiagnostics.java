package io.github.naimjeg.damagenexus.core.template;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.config.DiagnosticMode;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import io.github.naimjeg.damagenexus.diagnostics.logging.DiagnosticTextSanitizer;
import net.minecraft.resources.Identifier;

/** Rate-limited, opt-out diagnostics for unresolved payload-free references. */
public final class DamageTemplateDiagnostics {
    private DamageTemplateDiagnostics() {}

    public static void unresolved(
            String kind,
            Identifier id,
            long revision,
            String source
    ) {
        if (DamageNexusSettings.diagnosticMode() == DiagnosticMode.OFF) {
            return;
        }
        String safeKind = DiagnosticTextSanitizer.sanitizeLine(kind, 32);
        String safeId = DiagnosticTextSanitizer.sanitizeLine(
                String.valueOf(id), 256);
        if (DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.TEMPLATE_REFERENCE,
                safeKind + ":" + safeId,
                "resolve",
                Long.toString(revision))) {
            DamageNexus.LOGGER.warn(
                    "[DamageNexus] Unresolved static template reference skipped; kind={} id={} revision={} source={}",
                    DiagnosticTextSanitizer.sanitizeArguments(
                            safeKind, safeId, revision,
                            DiagnosticTextSanitizer.sanitizeLine(source, 128)));
        }
    }

    public static void incompatible(
            long templateRevision,
            long validatedChannelRevision,
            String source
    ) {
        if (DamageNexusSettings.diagnosticMode() == DiagnosticMode.OFF) {
            return;
        }
        String key = templateRevision + ":" + validatedChannelRevision;
        if (DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.TEMPLATE_REFERENCE,
                "registry_snapshot", "dependency_incompatible", key)) {
            DamageNexus.LOGGER.warn(
                    "[DamageNexus] Static template references skipped because the pinned registry snapshot is not server-authoritative; templateRevision={} validatedChannelRevision={} source={}",
                    DiagnosticTextSanitizer.sanitizeArguments(
                            templateRevision,
                            validatedChannelRevision,
                            DiagnosticTextSanitizer.sanitizeLine(source, 128)));
        }
    }
}
