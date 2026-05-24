package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.core.rule.StackingTrace;
import io.github.naimjeg.damagenexus.core.trace.DamageMutationType;
import io.github.naimjeg.damagenexus.core.trace.RuleSkipReason;

import java.util.List;

public final class NoOpCombatTrace implements CombatTrace {

    static final NoOpCombatTrace INSTANCE = new NoOpCombatTrace();

    private static final CombatTransactionLog TRANSACTION = new CombatTransactionLog() {
        @Override
        public void begin(String attackerName, String victimName, String sourceId, String initialChannel, float eventOriginalAmount, float initialBaseAmount) {
        }

        @Override
        public void preNexus(float amount) {
        }

        @Override
        public void apply(float eventOriginalAmount, float initialBaseAmount, float offensiveTotal, float finalEventAmount) {
        }

        @Override
        public void end() {
        }
    };

    private static final CombatPipelineLog PIPELINE = new CombatPipelineLog() {
        @Override
        public void layout(DamagePhase phase, List<DamagePhaseProcessor> processors) {
        }

        @Override
        public void phase(DamagePhase phase) {
        }

        @Override
        public void processorRun(DamagePhase phase, DamagePhaseProcessor processor) {
        }

        @Override
        public void processorSkip(DamagePhase phase, DamagePhaseProcessor processor) {
        }
    };

    private static final CombatRuleLog RULES = new CombatRuleLog() {
        @Override
        public void collected(DamagePhase phase, DamageRuleDefinition rule, RuleExecutionContext exec) {
        }

        @Override
        public void skipped(DamagePhase phase, DamageRuleDefinition rule, RuleSkipReason reason) {
        }

        @Override
        public void phaseMismatch(DamagePhase runningPhase, DamageRuleDefinition rule) {
        }

        @Override
        public void roleMismatch(DamagePhase phase, DamageRuleDefinition rule, RuleExecutionContext exec) {
        }

        @Override
        public void conditionFailed(DamagePhase phase, DamageRuleDefinition rule, DamageRuleCondition condition) {
        }

        @Override
        public void executed(DamagePhase phase, DamageRuleDefinition rule) {
        }

        @Override
        public void result(DamagePhase phase, DamageRuleDefinition rule, int appliedOperations, int rejectedOperations, int noOpOperations) {
        }

        @Override
        public void stackingDrop(StackingTrace trace) {
        }
    };

    private static final CombatMutationLog MUTATIONS = new CombatMutationLog() {
        @Override
        public void baseDamage(String sourceId, DamagePhase phase, DamageApplicationBucket applicationBucket, float value) {
        }

        @Override
        public void applicationPreMultiplier(String sourceId, DamagePhase phase, DamageApplicationBucket applicationBucket, int preMultiplierBucket, float value) {
        }

        @Override
        public void mutation(String sourceId, DamagePhase phase, DamageMutationType type, float value) {
        }

        @Override
        public void rejected(String action, DamagePhase currentPhase, String reason) {
        }
    };

    private static final CombatCalculationLog CALCULATION = new CombatCalculationLog() {
        @Override
        public void offenseStart() {
        }

        @Override
        public void channelResult(String channelId, float baseAmount, float result) {
        }

        @Override
        public void offensiveSummary(float total) {
        }

        @Override
        public void armor(String channelId, float damageBefore, float baseArmor, float armorEffectiveness, float effectiveArmor, float reductionPercent) {
        }

        @Override
        public void resistance(
                String channelId,
                float channelRating,
                float temporaryRating,
                float categoryRating,
                float totalRating,
                float reductionPercent
        ) {
        }

        @Override
        public void attributeScaling(
                String scope,
                String target,
                String attributeId,
                String bucketId,
                float additive
        ) {
        }

        @Override
        public void enchantmentProtection(String enchantmentId, int level, float scoreDelta, float ratingDelta) {
        }

        @Override
        public void defensiveSummary(float total) {
        }

        @Override
        public void bucketResult(String channelId, DamageApplicationBucket applicationBucket, float baseAmount, float offensiveAmount, float postMitigationAmount, boolean affectedByMitigation) {
        }

    };

    private static final CombatContributionLog NO_OP_CONTRIBUTIONS =
            new CombatContributionLog() {

                @Override
                public void summary(
                        int appliedCount,
                        int rejectedCount,
                        int noOpCount
                ) {
                }

                @Override
                public void applied(
                        io.github.naimjeg.damagenexus.api.display.DamageContributionDescriptor descriptor
                ) {
                }

                @Override
                public void rejected(
                        io.github.naimjeg.damagenexus.api.display.DamageContributionDescriptor descriptor
                ) {
                }
            };


    private NoOpCombatTrace() {
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public CombatTransactionLog transaction() {
        return TRANSACTION;
    }

    @Override
    public CombatPipelineLog pipeline() {
        return PIPELINE;
    }

    @Override
    public CombatRuleLog rules() {
        return RULES;
    }

    @Override
    public CombatMutationLog mutations() {
        return MUTATIONS;
    }

    @Override
    public CombatCalculationLog calculation() {
        return CALCULATION;
    }

    @Override
    public CombatContributionLog contributions() {
        return NO_OP_CONTRIBUTIONS;
    }
}

