package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.critical.*;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.damagenexus.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.damagenexus.core.critical.CriticalDecisionProviders;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import io.github.naimjeg.damagenexus.registry.PreMultiplierBuckets;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Executes the private critical-decision lifecycle between two public phases. */
final class CriticalDecisionEngine {
    private static final String FRAMEWORK_TRACE = "dn:critical_hit";
    private static final String MELEE_TRACE = "vanilla:critical_hit";
    private static final String PROJECTILE_TRACE = "vanilla:projectile_critical_bonus";
    private static final float DEFAULT_CRITICAL_INCREMENT = 0.5f;
    private static final float EPSILON = 0.0001f;
    private static final ThreadLocal<CriticalChanceSampler> TEST_SAMPLER =
            new ThreadLocal<>();

    private CriticalDecisionEngine() {
    }

    static void execute(DamageNexusContext context) {
        if (!context.claimCriticalProviderCollection()) {
            throw new IllegalStateException(
                    "Critical decision lifecycle already executed for this transaction");
        }
        CriticalDecisionProviders.collect(context);
        CriticalDecisionProviders.diagnoseHighestPriorityConflict(
                context.criticalDecisionContributions());
        CriticalDecision decision = context.resolveCriticalDecisionContributions();

        if (decision == CriticalDecision.SUPPRESS_CRITICAL) {
            context.freezeCriticalDecision(
                    decision, false, CriticalDecisionOutcome.SUPPRESSED, false);
            return;
        }

        boolean melee = context.hasCapturedMeleeCritical();
        boolean projectile = hasProjectileCritical(context);
        if (decision == CriticalDecision.FORCE_CRITICAL) {
            CriticalDecisionOutcome outcome = melee
                    ? CriticalDecisionOutcome.VANILLA_MELEE
                    : projectile
                    ? CriticalDecisionOutcome.VANILLA_PROJECTILE
                    : CriticalDecisionOutcome.FORCED;
            context.freezeCriticalDecision(decision, true, outcome, false);
            applyCriticalEffect(context, outcome);
            return;
        }

        if (projectile) {
            context.freezeCriticalDecision(
                    decision, true, CriticalDecisionOutcome.VANILLA_PROJECTILE, false);
            applyCriticalEffect(context, CriticalDecisionOutcome.VANILLA_PROJECTILE);
            return;
        }
        if (melee && !context.suppressesDefaultCritical()) {
            context.freezeCriticalDecision(
                    decision, true, CriticalDecisionOutcome.VANILLA_MELEE, false);
            applyCriticalEffect(context, CriticalDecisionOutcome.VANILLA_MELEE);
            return;
        }
        if (!(context.logicalAttacker() instanceof Player player)
                || context.suppressesDefaultCritical()) {
            context.freezeCriticalDecision(
                    decision, false, CriticalDecisionOutcome.INELIGIBLE, false);
            return;
        }

        ChanceRoll roll = rollChance(
                context.getAttackerAttrOrZero(ModAttributes.CRIT_CHANCE),
                () -> sampleChance(player));
        if (!roll.sampled()) {
            context.freezeCriticalDecision(
                    decision, false, CriticalDecisionOutcome.DEFAULT_NON_CRITICAL, false);
            return;
        }
        boolean critical = roll.critical();
        context.freezeCriticalDecision(
                decision,
                critical,
                critical ? CriticalDecisionOutcome.ATTRIBUTE_CHANCE
                        : CriticalDecisionOutcome.DEFAULT_NON_CRITICAL,
                true
        );
        if (critical) applyCriticalEffect(context, CriticalDecisionOutcome.ATTRIBUTE_CHANCE);
    }

    static float sanitizeChance(float chance) {
        if (!Float.isFinite(chance)) return 0.0f;
        return Math.clamp(chance, 0.0f, 1.0f);
    }

    static ChanceRoll rollChance(float chance, FloatSampler random) {
        float safeChance = sanitizeChance(chance);
        if (safeChance <= 0.0f) return new ChanceRoll(false, false);
        float roll = random.sample();
        return new ChanceRoll(true, Float.isFinite(roll) && roll < safeChance);
    }

    private static boolean hasProjectileCritical(DamageNexusContext context) {
        VanillaDamageCapture.OffensiveSnapshot snapshot = context.getVanillaSnapshot();
        return context.shouldRebuildVanillaPreEventDelta()
                && snapshot != null
                && snapshot.projectileCritical();
    }

    private static void applyCriticalEffect(
            DamageNexusContext context,
            CriticalDecisionOutcome outcome
    ) {
        if (context.criticalEffectApplied()) {
            throw new IllegalStateException("Critical effect already applied");
        }
        float additive = context.getAttackerAttrOrZero(
                ModAttributes.CRIT_DAMAGE_ADDITIVE);
        if (!Float.isFinite(additive)) additive = 0.0f;

        switch (outcome) {
            case VANILLA_MELEE -> applyVanillaMelee(context, additive);
            case VANILLA_PROJECTILE -> applyVanillaProjectile(context, additive);
            case FORCED, ATTRIBUTE_CHANCE -> context.tryAddGlobalPreMultiplier(
                    PreMultiplierBuckets.critDamage(),
                    DEFAULT_CRITICAL_INCREMENT + additive,
                    FRAMEWORK_TRACE
            );
            default -> throw new IllegalArgumentException(
                    "Cannot apply a critical effect for outcome " + outcome);
        }
        context.markCriticalEffectApplied();
    }

    private static void applyVanillaMelee(DamageNexusContext context, float additive) {
        float capturedIncrement = context.vanillaCriticalMultiplier() - 1.0f;
        float value = capturedIncrement + additive;
        recordMeleeMultiplier(context, DamageApplicationBucket.VANILLA_MELEE_BASE, value);
        recordMeleeMultiplier(context, DamageApplicationBucket.VANILLA_MELEE_ENCHANTMENT, value);
        recordMeleeMultiplier(context, DamageApplicationBucket.VANILLA_WEAPON_SPECIAL, value);
    }

    private static void recordMeleeMultiplier(
            DamageNexusContext context,
            DamageApplicationBucket bucket,
            float value
    ) {
        DamageMutationResult result = context.tryAddApplicationPreMultiplier(
                bucket, PreMultiplierBuckets.critDamage(), value, MELEE_TRACE);
        context.contributions().record(result, () ->
                VanillaContributionDescriptors.vanillaMultiplier(
                        DamageNexusIds.id("vanilla_critical_hit/"
                                + bucket.name().toLowerCase(Locale.ROOT)),
                        DamagePhase.CRITICAL_HIT,
                        context.getInitialChannel().id(), bucket,
                        DamageNexusPreMultiplierBuckets.CRIT_DAMAGE, value, MELEE_TRACE));
    }

    private static void applyVanillaProjectile(DamageNexusContext context, float additive) {
        VanillaDamageCapture.OffensiveSnapshot snapshot = context.getVanillaSnapshot();
        float bonus = snapshot.projectileCriticalBonus();
        DamageApplicationBucket bonusBucket =
                DamageApplicationBucket.VANILLA_PROJECTILE_CRIT_BONUS;
        DamageMutationResult result = context.tryAddVanillaCriticalBonusDamage(
                context.getInitialChannel(), bonusBucket, bonus, PROJECTILE_TRACE);
        context.contributions().record(result, () ->
                VanillaContributionDescriptors.vanillaBase(
                        DamageNexusIds.id("vanilla_projectile_critical_bonus"),
                        DamagePhase.CRITICAL_HIT, context.getInitialChannel().id(),
                        bonusBucket, bonus, PROJECTILE_TRACE));

        if (Math.abs(additive) > EPSILON) {
            context.tryAddApplicationPreMultiplier(
                    DamageApplicationBucket.VANILLA_PROJECTILE_BASE,
                    PreMultiplierBuckets.critDamage(), additive, FRAMEWORK_TRACE);
            context.tryAddApplicationPreMultiplier(
                    DamageApplicationBucket.VANILLA_PROJECTILE_ENCHANTMENT,
                    PreMultiplierBuckets.critDamage(), additive, FRAMEWORK_TRACE);
        }
    }

    private static float sampleChance(Player attacker) {
        CriticalChanceSampler testSampler = TEST_SAMPLER.get();
        return testSampler == null
                ? attacker.getRandom().nextFloat()
                : testSampler.sample(attacker);
    }

    static SamplerScope useSamplerForTesting(CriticalChanceSampler replacement) {
        if (replacement == null) {
            throw new NullPointerException("replacement");
        }
        if (TEST_SAMPLER.get() != null) {
            throw new IllegalStateException(
                    "A critical chance test sampler is already active on this thread");
        }
        TEST_SAMPLER.set(replacement);
        return new SamplerScope(Thread.currentThread());
    }

    static void resetSamplerForTesting() {
        TEST_SAMPLER.remove();
    }

    static boolean hasTestSamplerForTesting() {
        return TEST_SAMPLER.get() != null;
    }

    @FunctionalInterface
    interface CriticalChanceSampler {
        float sample(Player attacker);
    }

    static final class SamplerScope implements AutoCloseable {
        private final Thread owner;
        private boolean closed;

        private SamplerScope(Thread owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException(
                        "Critical chance test sampler scope must close on its owner thread");
            }
            TEST_SAMPLER.remove();
            closed = true;
        }
    }

    @FunctionalInterface
    interface FloatSampler {
        float sample();
    }

    record ChanceRoll(boolean sampled, boolean critical) { }
}
