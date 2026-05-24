package io.github.naimjeg.damagenexus.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** Operational rule callback failures; intentionally independent of diagnostic mode. */
public final class RuleExecutionDiagnosticsLog {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RuleExecutionDiagnosticsLog() {
    }

    public static void error(
            Identifier ruleId,
            DamagePhase phase,
            String stage,
            Exception exception
    ) {
        error(LOGGER, ruleId, phase, stage, exception);
    }

    static boolean error(
            Logger logger,
            Identifier ruleId,
            DamagePhase phase,
            String stage,
            Exception exception
    ) {
        String safeRuleId = ruleId == null ? "<unknown_rule>" : ruleId.toString();
        String safePhase = phase == null ? "<unknown_phase>" : phase.name();
        String exceptionClass = exception.getClass().getName();

        if (!DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.RULE_EXECUTION,
                safeRuleId,
                safePhase + "/" + stage,
                exceptionClass
        )) {
            return false;
        }

        logger.error(
                DiagnosticTextSanitizer.sanitizeLine(
                        "[DamageNexus] Rule callback failed: rule={} phase={} stage={} exception_type={}"
                ),
                DiagnosticTextSanitizer.sanitizeArguments(
                        safeRuleId,
                        safePhase,
                        stage,
                        exceptionClass,
                        exception
                )
        );
        return true;
    }
}
