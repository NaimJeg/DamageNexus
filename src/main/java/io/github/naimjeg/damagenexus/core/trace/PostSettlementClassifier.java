package io.github.naimjeg.damagenexus.core.trace;

import io.github.naimjeg.damagenexus.config.VanillaReductionCompatibilityMode;

/** Pure, logger-independent classification of staged damage observations. */
public final class PostSettlementClassifier {

    private static final float EPSILON = 0.001f;

    private PostSettlementClassifier() {
    }

    public static Result classify(
            DamageNexusTransaction tx,
            PostSettlementObservation post
    ) {
        float observed = post.observedTotalDelta(tx);

        if (!finite(
                tx.eventAmountAfterSet(),
                tx.preNewDamage(),
                post.postInflictedDamage(),
                post.postHealthDamage(),
                observed
        )) {
            return mismatch(Kind.NON_FINITE_OBSERVATION, tx, post, observed);
        }

        if (suppressedReductionObserved(tx)) {
            return mismatch(
                    Kind.SUPPRESSED_REDUCTION_OBSERVED,
                    tx,
                    post,
                    observed
            );
        }

        float preStageDelta = tx.eventAmountAfterSet() - tx.preNewDamage();
        boolean preStageChanged = !close(preStageDelta, 0.0f);
        boolean expectedReduction = allowedReductionObserved(tx)
                || tx.blockedDamage() > EPSILON
                || tx.invulnerabilityReduction() > EPSILON
                || tx.knownPreStageAdjustment();

        if (preStageChanged
                && allConfigurableReductionsSuppressed(tx)
                && !expectedReduction) {
            return mismatch(
                    Kind.UNEXPLAINED_PRE_STAGE_CHANGE,
                    tx,
                    post,
                    observed
            );
        }

        if (!close(tx.preNewDamage(), post.postInflictedDamage())) {
            return mismatch(
                    Kind.PRE_TO_POST_AMOUNT_CHANGED,
                    tx,
                    post,
                    observed
            );
        }

        float attributedPostDamage = Math.max(0.0f, post.postHealthDamage())
                + Math.min(
                        Math.max(0.0f, post.absorptionReduction()),
                        Math.max(0.0f, post.postInflictedDamage())
                );
        if (!close(attributedPostDamage, post.postInflictedDamage())) {
            return mismatch(
                    Kind.POST_COMPONENTS_INCONSISTENT,
                    tx,
                    post,
                    observed
            );
        }

        float expectedObserved = expectedObservedDelta(tx, post);

        if (close(observed, expectedObserved)) {
            if (post.postHealthDamage()
                    > tx.victimHealthBefore() + EPSILON) {
                return expected(Kind.OVERKILL_CAP, tx, post, observed);
            }
            if (post.absorptionReduction() > EPSILON
                    || post.absorptionDelta(tx) > EPSILON) {
                return expected(Kind.ABSORPTION, tx, post, observed);
            }
            if (tx.invulnerabilityReduction() > EPSILON) {
                return expected(
                        Kind.VANILLA_INVULNERABILITY_ADJUSTMENT,
                        tx,
                        post,
                        observed
                );
            }
            if (preStageChanged) {
                return expected(
                        expectedReduction
                                ? Kind.VANILLA_OR_EXTERNAL_ADJUSTMENT
                                : Kind.PRE_STAGE_ADJUSTMENT,
                        tx,
                        post,
                        observed
                );
            }
            if (post.postInflictedDamage() <= EPSILON) {
                return expected(Kind.LATE_ZERO_DAMAGE, tx, post, observed);
            }
            return consistent(tx, post, observed);
        }

        if (tx.victimInvulnerableTimeBefore() > 0) {
            return expected(
                    observed > expectedObserved
                            ? Kind.BATCHED_OBSERVED_DELTA
                            : Kind.VANILLA_INVULNERABILITY_ADJUSTMENT,
                    tx,
                    post,
                    observed
            );
        }

        return mismatch(Kind.POST_SETTLEMENT_DELTA, tx, post, observed);
    }

    private static boolean suppressedReductionObserved(DamageNexusTransaction tx) {
        return tx.suppressArmor() && tx.armorReduction() > EPSILON
                || tx.suppressEnchantments() && tx.enchantmentReduction() > EPSILON
                || tx.suppressMobEffects() && tx.mobEffectReduction() > EPSILON
                || tx.suppressInnateResistance()
                && tx.innateResistanceReduction() > EPSILON;
    }

    private static boolean allowedReductionObserved(DamageNexusTransaction tx) {
        return !tx.suppressArmor() && tx.armorReduction() > EPSILON
                || !tx.suppressEnchantments() && tx.enchantmentReduction() > EPSILON
                || !tx.suppressMobEffects() && tx.mobEffectReduction() > EPSILON
                || !tx.suppressInnateResistance()
                && tx.innateResistanceReduction() > EPSILON;
    }

    private static boolean allConfigurableReductionsSuppressed(
            DamageNexusTransaction tx
    ) {
        return tx.vanillaReductionMode()
                != VanillaReductionCompatibilityMode.COOPERATIVE
                && tx.suppressArmor()
                && tx.suppressEnchantments()
                && tx.suppressMobEffects()
                && tx.suppressInnateResistance();
    }

    private static Result consistent(
            DamageNexusTransaction tx,
            PostSettlementObservation post,
            float observed
    ) {
        return result(Severity.CONSISTENT, Kind.NONE, tx, post, observed);
    }

    private static Result expected(
            Kind kind,
            DamageNexusTransaction tx,
            PostSettlementObservation post,
            float observed
    ) {
        return result(Severity.EXPECTED_ADJUSTMENT, kind, tx, post, observed);
    }

    private static Result mismatch(
            Kind kind,
            DamageNexusTransaction tx,
            PostSettlementObservation post,
            float observed
    ) {
        return result(Severity.MISMATCH, kind, tx, post, observed);
    }

    private static Result result(
            Severity severity,
            Kind kind,
            DamageNexusTransaction tx,
            PostSettlementObservation post,
            float observed
    ) {
        return new Result(
                severity,
                kind,
                tx.eventAmountAfterSet(),
                tx.preNewDamage(),
                post.postInflictedDamage(),
                expectedObservedDelta(tx, post),
                observed,
                tx.eventAmountAfterSet() - tx.preNewDamage(),
                tx.preNewDamage() - post.postInflictedDamage(),
                post.postInflictedDamage() - observed
        );
    }

    private static boolean close(float left, float right) {
        return Math.abs(left - right) <= EPSILON;
    }

    private static float expectedObservedDelta(
            DamageNexusTransaction tx,
            PostSettlementObservation post
    ) {
        float expectedHealth = Math.min(
                Math.max(0.0f, post.postHealthDamage()),
                Math.max(0.0f, tx.victimHealthBefore())
        );
        float expectedAbsorption = Math.min(
                Math.max(0.0f, post.absorptionReduction()),
                Math.min(
                        Math.max(0.0f, post.postInflictedDamage()),
                        Math.max(0.0f, tx.victimAbsorptionBefore())
                )
        );
        return expectedHealth + expectedAbsorption;
    }

    private static boolean finite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    public enum Severity {
        CONSISTENT,
        EXPECTED_ADJUSTMENT,
        MISMATCH
    }

    public enum Kind {
        NONE,
        PRE_STAGE_ADJUSTMENT,
        VANILLA_OR_EXTERNAL_ADJUSTMENT,
        ABSORPTION,
        OVERKILL_CAP,
        VANILLA_INVULNERABILITY_ADJUSTMENT,
        LATE_ZERO_DAMAGE,
        BATCHED_OBSERVED_DELTA,
        SUPPRESSED_REDUCTION_OBSERVED,
        UNEXPLAINED_PRE_STAGE_CHANGE,
        PRE_TO_POST_AMOUNT_CHANGED,
        POST_COMPONENTS_INCONSISTENT,
        POST_SETTLEMENT_DELTA,
        NON_FINITE_OBSERVATION
    }

    public record Result(
            Severity severity,
            Kind kind,
            float expectedDamage,
            float preDamage,
            float postInflictedDamage,
            float expectedObservedDelta,
            float observedTotalDelta,
            float incomingToPreDelta,
            float preToPostDelta,
            float postToObservedDelta
    ) {
        public boolean mismatch() {
            return severity == Severity.MISMATCH;
        }

        public boolean adjusted() {
            return severity == Severity.EXPECTED_ADJUSTMENT;
        }
    }
}
