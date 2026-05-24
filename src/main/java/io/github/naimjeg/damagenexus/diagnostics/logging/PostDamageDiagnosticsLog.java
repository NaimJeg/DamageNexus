package io.github.naimjeg.damagenexus.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.core.trace.DamageNexusTransaction;
import io.github.naimjeg.damagenexus.core.trace.PostSettlementClassifier;
import io.github.naimjeg.damagenexus.core.trace.PostSettlementObservation;
import net.minecraft.world.damagesource.DamageSource;
import org.slf4j.Logger;

public final class PostDamageDiagnosticsLog {

    private static final Logger LOGGER = LogUtils.getLogger();

    private PostDamageDiagnosticsLog() {
    }

    public static void observed(
            DamageNexusTransaction tx,
            PostSettlementObservation post,
            PostSettlementClassifier.Result result
    ) {
        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_SUMMARY,
                LOGGER,
                template("POST_SUMMARY"),
                arguments(tx, post, result, "")
        );
    }

    public static void adjusted(
            DamageNexusTransaction tx,
            PostSettlementObservation post,
            PostSettlementClassifier.Result result
    ) {
        if (!DamageNexusLogSink.shouldAccept(DamageNexusLogKind.COMPATIBILITY)) {
            return;
        }
        CompatibilityDiagnosticRateLimiter.Decision decision =
                CompatibilityDiagnosticRateLimiter.check(
                        "post_settlement/adjusted/"
                                + result.kind()
                                + "/"
                                + sourceId(tx.source())
                );
        if (!decision.allowed()) {
            return;
        }

        DamageNexusLogSink.info(
                DamageNexusLogKind.COMPATIBILITY,
                LOGGER,
                template("POST_ADJUSTED"),
                arguments(tx, post, result, decision.suffix())
        );
    }

    public static void mismatch(
            DamageNexusTransaction tx,
            PostSettlementObservation post,
            PostSettlementClassifier.Result result
    ) {
        String source = sourceId(tx.source());
        if (!DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.POST_SETTLEMENT,
                source,
                "post/" + result.kind(),
                tx.vanillaReductionMode().name()
        )) {
            return;
        }

        LOGGER.warn(
                DiagnosticTextSanitizer.sanitizeLine(template("POST_MISMATCH")),
                DiagnosticTextSanitizer.sanitizeArguments(
                        arguments(tx, post, result, "")
                )
        );
    }

    private static String template(String marker) {
        return "[DN#{}] " + marker
                + " kind={} source={} mode={} incoming_original={} incoming_before_dn={} incoming_after_dn={}"
                + " expected_dn={} pre_new={} post_inflicted={} post_health_damage={} expected_observed={} observed_total={}"
                + " incoming_to_pre_delta={} pre_to_post_delta={} post_to_observed_delta={}"
                + " blocked={} reduction_invulnerability={} reduction_armor={} reduction_enchantments={}"
                + " reduction_mob_effects={} reduction_innate={} reduction_absorption={}"
                + " known_pre_stage_adjustment={}"
                + " health_before={} health_after={} absorption_before={} absorption_after={}"
                + " invul_before={} invul_after={}{}";
    }

    private static Object[] arguments(
            DamageNexusTransaction tx,
            PostSettlementObservation post,
            PostSettlementClassifier.Result result,
            String suffix
    ) {
        return new Object[]{
                tx.damageId(),
                result.kind(),
                sourceId(tx.source()),
                tx.vanillaReductionMode(),
                tx.eventOriginalAmount(),
                tx.eventAmountBeforeSet(),
                tx.eventAmountAfterSet(),
                result.expectedDamage(),
                tx.preNewDamage(),
                post.postInflictedDamage(),
                post.postHealthDamage(),
                result.expectedObservedDelta(),
                post.observedTotalDelta(tx),
                result.incomingToPreDelta(),
                result.preToPostDelta(),
                result.postToObservedDelta(),
                tx.blockedDamage(),
                tx.invulnerabilityReduction(),
                tx.armorReduction(),
                tx.enchantmentReduction(),
                tx.mobEffectReduction(),
                tx.innateResistanceReduction(),
                post.absorptionReduction(),
                tx.knownPreStageAdjustment(),
                tx.victimHealthBefore(),
                post.healthAfter(),
                tx.victimAbsorptionBefore(),
                post.absorptionAfter(),
                tx.victimInvulnerableTimeBefore(),
                post.invulnerableTimeAfter(),
                suffix
        };
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
}
