package io.github.naimjeg.damagenexus.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.core.trace.DamageNexusTransaction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public final class TransactionDiagnosticsLog {

    private static final Logger LOGGER = LogUtils.getLogger();

    private TransactionDiagnosticsLog() {
    }

    public static void drop(
            String reason,
            DamageNexusTransaction tx,
            DamageSource wantedSource,
            float eventInflictedDamage
    ) {
        DamageNexusLogKind kind = dropKind(reason);
        if (!DamageNexusLogSink.shouldAccept(kind)) {
            return;
        }

        CompatibilityDiagnosticRateLimiter.Decision decision =
                kind == DamageNexusLogKind.COMPATIBILITY
                        ? CompatibilityDiagnosticRateLimiter.check(
                                "transaction/drop/" + reason + "/" + sourceId(tx.source())
                        )
                        : null;
        if (decision != null && !decision.allowed()) {
            return;
        }

        DamageNexusLogSink.info(
                kind,
                LOGGER,
                "[DN-TX] DROP reason={} tx_id={} victim={} tx_source={} pre_new={} wanted_source={} post_inflicted={} tx_game_time={}{}",
                reason,
                tx.damageId(),
                tx.victim().getName().getString(),
                sourceId(tx.source()),
                tx.preNewDamage(),
                sourceId(wantedSource),
                eventInflictedDamage,
                tx.gameTime(),
                decision == null ? "" : decision.suffix()
        );
    }

    public static void lateAmountMatch(
            String label,
            DamageNexusTransaction tx,
            DamageSource wantedSource,
            float eventInflictedDamage,
            float diff,
            int staleDropped
    ) {
        if (!DamageNexusLogSink.shouldAccept(DamageNexusLogKind.COMPATIBILITY)) {
            return;
        }
        CompatibilityDiagnosticRateLimiter.Decision decision =
                CompatibilityDiagnosticRateLimiter.check(
                        "transaction/heuristic/" + label + "/" + sourceId(wantedSource)
                );
        if (!decision.allowed()) {
            return;
        }

        DamageNexusLogSink.info(
                DamageNexusLogKind.COMPATIBILITY,
                LOGGER,
                "[DN-TX] HEURISTIC_MATCH kind={} tx_id={} victim={} tx_source={} pre_new={} wanted_source={} post_inflicted={} diff={} invul_before={} stale_dropped={} tx_game_time={}{}",
                label,
                tx.damageId(),
                tx.victim().getName().getString(),
                sourceId(tx.source()),
                tx.preNewDamage(),
                sourceId(wantedSource),
                eventInflictedDamage,
                diff,
                tx.victimInvulnerableTimeBefore(),
                staleDropped,
                tx.gameTime(),
                decision.suffix()
        );
    }

    public static void ambiguousLateAmountMatch(
            String wantedSourceId,
            DamageNexusTransaction first,
            DamageNexusTransaction second
    ) {
        if (!DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.TRANSACTION_CORRELATION,
                wantedSourceId,
                "post_fallback",
                "ambiguous"
        )) {
            return;
        }

        warn(
                "[DN-TX] AMBIGUOUS_HEURISTIC_MATCH wanted_source={} first_tx={} first_pre={} second_tx={} second_pre={}",
                wantedSourceId,
                first.damageId(),
                first.preNewDamage(),
                second.damageId(),
                second.preNewDamage()
        );
    }

    public static void candidateRecord(DamageNexusTransaction tx) {
        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-TX] CANDIDATE_RECORD id={} victim={} source={} gt={} incoming_after_dn={}",
                tx.damageId(),
                tx.victim().getName().getString(),
                sourceId(tx.source()),
                tx.gameTime(),
                tx.eventAmountAfterSet()
        );
    }

    public static void candidatePromote(DamageNexusTransaction tx) {
        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_SUMMARY,
                LOGGER,
                "[DN-TX] CANDIDATE_PROMOTE id={} victim={} source={} incoming_original={} incoming_before_dn={} incoming_after_dn={} pre_new={} pre_health={} pre_absorption={} pre_invul={} gt={}",
                tx.damageId(),
                tx.victim().getName().getString(),
                sourceId(tx.source()),
                tx.eventOriginalAmount(),
                tx.eventAmountBeforeSet(),
                tx.eventAmountAfterSet(),
                tx.preNewDamage(),
                tx.victimHealthBefore(),
                tx.victimAbsorptionBefore(),
                tx.victimInvulnerableTimeBefore(),
                tx.gameTime()
        );
    }

    public static void postCorrelated(
            DamageNexusTransaction tx,
            String strategy,
            int remaining
    ) {
        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-TX] POST_CORRELATE id={} strategy={} victim={} source={} pre_new={} remaining={}",
                tx.damageId(),
                strategy,
                tx.victim().getName().getString(),
                sourceId(tx.source()),
                tx.preNewDamage(),
                remaining
        );
    }

    public static void preWithoutCandidate(
            LivingEntity victim,
            DamageSource source,
            float preDamage,
            long gameTime
    ) {
        String sourceIdentity = sourceId(source);
        if (!DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.TRANSACTION_CORRELATION,
                sourceIdentity,
                "pre",
                "missing_incoming_candidate"
        )) {
            return;
        }

        warn(
                "[DN-TX] PRE_WITHOUT_CANDIDATE victim={} source={} pre_new={} gt={}",
                victim.getName().getString(),
                sourceIdentity,
                preDamage,
                gameTime
        );
    }

    public static void candidatePrune(int removed, int remaining, long now) {
        if (removed <= 0) {
            return;
        }
        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-TX] CANDIDATE_PRUNE removed={} remaining={} gt={}",
                removed,
                remaining,
                now
        );
    }

    private static void warn(String template, Object... args) {
        LOGGER.warn(
                DiagnosticTextSanitizer.sanitizeLine(template),
                DiagnosticTextSanitizer.sanitizeArguments(args)
        );
    }

    private static String sourceId(DamageSource source) {
        if (source == null) {
            return "null";
        }
        return source.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unregistered");
    }

    private static DamageNexusLogKind dropKind(String reason) {
        return reason != null && reason.startsWith("stale_before")
                ? DamageNexusLogKind.COMPATIBILITY
                : DamageNexusLogKind.TRACE_DETAIL;
    }
}
