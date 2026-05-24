package io.github.naimjeg.damagenexus.bridge.vanilla;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import org.jspecify.annotations.Nullable;

public record VanillaBridgePlan(
        float initialBaseAmount,
        DamageApplicationBucket initialBaseBucket,

        boolean rebuildOffensiveMobEffects,
        DamageApplicationBucket offensiveMobEffectBucket,

        boolean rebuildOffensiveEnchantment,
        DamageApplicationBucket offensiveEnchantmentBucket,

        boolean rebuildPreEventDelta,
        float offensiveMobEffectDelta,

        String reason,
        String baseTraceId
) {
    private static final float EPSILON = 0.0001f;

    public static VanillaBridgePlan from(
            float eventOriginalAmount,
            VanillaDamageSourceProfile profile,
            VanillaDamageCapture.@Nullable OffensiveSnapshot snapshot,
            float observedOffensiveMobEffectDelta,
            float enabledOffensiveMobEffectDelta
    ) {
        DamageApplicationBucket initialBaseBucket =
                initialBaseBucket(profile);

        DamageApplicationBucket mobEffectBucket =
                DamageApplicationBucket.VANILLA_MELEE_BASE;

        DamageApplicationBucket enchantmentBucket =
                offensiveEnchantmentBucket(profile);

        boolean hasObservedMobEffectDelta =
                Float.isFinite(observedOffensiveMobEffectDelta)
                        && Math.abs(observedOffensiveMobEffectDelta) > EPSILON;

        boolean hasEnabledMobEffectDelta =
                Float.isFinite(enabledOffensiveMobEffectDelta)
                        && Math.abs(enabledOffensiveMobEffectDelta) > EPSILON;

        if (snapshot == null) {
            if (hasObservedMobEffectDelta || hasEnabledMobEffectDelta) {
                String reason = hasEnabledMobEffectDelta
                        ? "rebuild_mob_effect_no_snapshot"
                        : "rebuild_observed_mob_effect_base_only_no_snapshot";

                return plan(
                        removeMobEffectDelta(
                                eventOriginalAmount,
                                observedOffensiveMobEffectDelta
                        ),
                        initialBaseBucket,

                        hasEnabledMobEffectDelta,
                        mobEffectBucket,

                        false,
                        enchantmentBucket,

                        false,
                        hasEnabledMobEffectDelta
                                ? enabledOffensiveMobEffectDelta
                                : 0.0f,

                        reason,
                        "vanilla:base/fallback/no_snapshot_mob_effect_adjusted"
                );
            }

            return plan(
                    eventOriginalAmount,
                    initialBaseBucket,

                    false,
                    mobEffectBucket,

                    false,
                    enchantmentBucket,

                    false,
                    0.0f,

                    "no_snapshot",
                    "vanilla:base/fallback/no_snapshot"
            );
        }

        boolean hasEnchantDelta = snapshot.hasEnchantDelta();

        boolean hasBridgeablePreEventDelta =
                snapshot.preEventDelta().kind() != PreEventDeltaKind.NONE
                        && isBridgeablePreEventDelta(snapshot.preEventDelta().kind())
                        && isUsableRatio(snapshot);

        boolean hasUnbridgeablePreEventDelta =
                snapshot.preEventDelta().kind() != PreEventDeltaKind.NONE
                        && !isBridgeablePreEventDelta(snapshot.preEventDelta().kind());

        /*
         * A vanilla critical arrow can legally roll a zero-point random bonus.
         * The captured critical flag, rather than the numeric bonus, is therefore
         * the authoritative reason to rebuild this pre-event contribution.  This
         * also prevents the critical-decision engine from falling through to a
         * second attribute-chance sample on that zero-bonus edge case.
         */
        boolean hasProjectileCritical = snapshot.projectileCritical();

        boolean rebuildPreEvent =
                hasBridgeablePreEventDelta || hasProjectileCritical;

        /*
         * Important:
         * If pre-event delta is unbridgeable, preserve vanilla amount as-is.
         * Do not partially rebuild mob effects, otherwise the unknown vanilla
         * multiplier/order may change final damage.
         */
        if (hasUnbridgeablePreEventDelta) {
            return plan(
                    eventOriginalAmount,
                    initialBaseBucket,

                    false,
                    mobEffectBucket,

                    false,
                    enchantmentBucket,

                    false,
                    0.0f,

                    "fallback_unbridgeable_pre_event kind="
                            + snapshot.preEventDelta().kind(),
                    "vanilla:base/fallback/unbridgeable_pre_event"
            );
        }

        if (hasEnchantDelta) {
            return plan(
                    removeMobEffectDelta(
                            snapshot.preEnchantDamage(),
                            observedOffensiveMobEffectDelta
                    ),
                    initialBaseBucket,

                    hasEnabledMobEffectDelta,
                    mobEffectBucket,

                    true,
                    enchantmentBucket,

                    rebuildPreEvent,
                    hasEnabledMobEffectDelta
                            ? enabledOffensiveMobEffectDelta
                            : 0.0f,

                    reason(
                            hasObservedMobEffectDelta,
                            hasEnabledMobEffectDelta,
                            true,
                            rebuildPreEvent
                    ),
                    baseTraceId(initialBaseBucket)
            );
        }

        if (rebuildPreEvent) {
            return plan(
                    removeMobEffectDelta(
                            snapshot.postEnchantDamage(),
                            observedOffensiveMobEffectDelta
                    ),
                    initialBaseBucket,

                    hasEnabledMobEffectDelta,
                    mobEffectBucket,

                    false,
                    enchantmentBucket,

                    true,
                    hasEnabledMobEffectDelta
                            ? enabledOffensiveMobEffectDelta
                            : 0.0f,

                    reason(
                            hasObservedMobEffectDelta,
                            hasEnabledMobEffectDelta,
                            false,
                            true
                    ),
                    baseTraceId(initialBaseBucket)
            );
        }

        if (hasObservedMobEffectDelta || hasEnabledMobEffectDelta) {
            return plan(
                    removeMobEffectDelta(
                            eventOriginalAmount,
                            observedOffensiveMobEffectDelta
                    ),
                    initialBaseBucket,

                    hasEnabledMobEffectDelta,
                    mobEffectBucket,

                    false,
                    enchantmentBucket,

                    false,
                    hasEnabledMobEffectDelta
                            ? enabledOffensiveMobEffectDelta
                            : 0.0f,

                    reason(
                            hasObservedMobEffectDelta,
                            hasEnabledMobEffectDelta,
                            false,
                            false
                    ),
                    baseTraceId(initialBaseBucket)
            );
        }

        return plan(
                eventOriginalAmount,
                initialBaseBucket,

                false,
                mobEffectBucket,

                false,
                enchantmentBucket,

                false,
                0.0f,

                "no_rebuild_needed",
                baseTraceId(initialBaseBucket)
        );
    }

    private static VanillaBridgePlan plan(
            float initialBaseAmount,
            DamageApplicationBucket initialBaseBucket,

            boolean rebuildOffensiveMobEffects,
            DamageApplicationBucket offensiveMobEffectBucket,

            boolean rebuildOffensiveEnchantment,
            DamageApplicationBucket offensiveEnchantmentBucket,

            boolean rebuildPreEventDelta,
            float offensiveMobEffectDelta,

            String reason,
            String baseTraceId
    ) {
        return new VanillaBridgePlan(
                initialBaseAmount,
                initialBaseBucket,

                rebuildOffensiveMobEffects,
                offensiveMobEffectBucket,

                rebuildOffensiveEnchantment,
                offensiveEnchantmentBucket,

                rebuildPreEventDelta,
                offensiveMobEffectDelta,

                reason,
                baseTraceId
        );
    }

    private static float removeMobEffectDelta(
            float amount,
            float observedMobEffectDelta
    ) {
        float result = amount - observedMobEffectDelta;

        if (!Float.isFinite(result)) {
            return amount;
        }

        return Math.max(0.0f, result);
    }

    private static String reason(
            boolean observedMobEffect,
            boolean enabledMobEffect,
            boolean enchant,
            boolean preEvent
    ) {
        StringBuilder sb = new StringBuilder("rebuild");

        if (enabledMobEffect) {
            sb.append("_mob_effect");
        } else if (observedMobEffect) {
            sb.append("_observed_mob_effect_base_only");
        }

        if (enchant) {
            sb.append("_enchant");
        }

        if (preEvent) {
            sb.append("_pre_event");
        }

        if (sb.length() == "rebuild".length()) {
            return "no_rebuild_needed";
        }

        return sb.toString();
    }

    private static boolean isUsableRatio(
            VanillaDamageCapture.OffensiveSnapshot snapshot
    ) {
        float ratio = snapshot.preEventDelta().ratio();

        return Float.isFinite(ratio)
                && Math.abs(ratio) > EPSILON;
    }

    private static boolean isBridgeablePreEventDelta(
            PreEventDeltaKind kind
    ) {
        return switch (kind) {
            case DIFFICULTY_SCALING,
                 PLAYER_ATTACK_SCALING,
                 PROJECTILE_SCALING,
                 SPECIAL_ATTACK_SCALING,
                 SPEAR_STAB_BONUS,
                 SPEAR_CHARGE_BONUS,
                 SPEAR_ATTACK_BONUS -> true;

            case NONE,
                 UNKNOWN -> false;
        };
    }

    private static DamageApplicationBucket initialBaseBucket(
            VanillaDamageSourceProfile profile
    ) {
        if (profile == null) {
            return DamageApplicationBucket.VANILLA_OTHER_BASE;
        }

        if (profile.projectile()) {
            return DamageApplicationBucket.VANILLA_PROJECTILE_BASE;
        }

        if (profile.shouldApplyMeleeOffensiveMobEffects()
                || profile.directLivingAttack()) {
            return DamageApplicationBucket.VANILLA_MELEE_BASE;
        }

        return DamageApplicationBucket.VANILLA_OTHER_BASE;
    }

    private static DamageApplicationBucket offensiveEnchantmentBucket(
            VanillaDamageSourceProfile profile
    ) {
        if (profile != null && profile.projectile()) {
            return DamageApplicationBucket.VANILLA_PROJECTILE_ENCHANTMENT;
        }

        return DamageApplicationBucket.VANILLA_MELEE_ENCHANTMENT;
    }

    private static String baseTraceId(
            DamageApplicationBucket bucket
    ) {
        return switch (bucket) {
            case VANILLA_MELEE_BASE -> "vanilla:base/melee";
            case VANILLA_PROJECTILE_BASE -> "vanilla:base/projectile";
            case VANILLA_OTHER_BASE -> "vanilla:base/other";
            default -> "vanilla:base/unknown";
        };
    }
}
