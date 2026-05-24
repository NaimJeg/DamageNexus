package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import org.slf4j.Logger;

/** Mode-gated server diagnostic output. Operational warnings/errors bypass this sink. */
public final class DamageNexusLogSink {

    private DamageNexusLogSink() {
    }

    public static void info(Logger logger, String template, Object... args) {
        info(DamageNexusLogKind.TRACE_DETAIL, logger, template, args);
    }

    public static void info(
            DamageNexusLogKind kind,
            Logger logger,
            String template,
            Object... args
    ) {
        if (!shouldAccept(kind)) {
            return;
        }

        logger.info(
                DiagnosticTextSanitizer.sanitizeLine(template),
                DiagnosticTextSanitizer.sanitizeArguments(args)
        );
    }

    public static boolean shouldAccept(DamageNexusLogKind kind) {
        DamageNexusLogKind effectiveKind = kind == null
                ? DamageNexusLogKind.TRACE_DETAIL
                : kind;
        return DamageNexusSettings.shouldEmitServer(effectiveKind);
    }
}
