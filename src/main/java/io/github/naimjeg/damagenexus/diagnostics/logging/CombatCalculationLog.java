package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;

public interface CombatCalculationLog {

    void offenseStart();

    void channelResult(
            String channelId,
            float baseAmount,
            float result
    );

    void offensiveSummary(float total);

    void armor(
            String channelId,
            float damageBefore,
            float baseArmor,
            float armorEffectiveness,
            float effectiveArmor,
            float reductionPercent
    );

    /** Detailed resistance composition. */
    void resistance(
            String channelId,
            float channelRating,
            float temporaryRating,
            float categoryRating,
            float totalRating,
            float reductionPercent
    );

    /** Full-trace-only attribute scaling contribution. */
    void attributeScaling(
            String scope,
            String target,
            String attributeId,
            String bucketId,
            float additive
    );

    void enchantmentProtection(
            String enchantmentId,
            int level,
            float scoreDelta,
            float ratingDelta
    );

    void defensiveSummary(float total);

    void bucketResult(
            String channelId,
            DamageApplicationBucket applicationBucket,
            float baseAmount,
            float offensiveAmount,
            float postMitigationAmount,
            boolean affectedByMitigation
    );

}

