package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.display.DamageContributionDescriptor;
import io.github.naimjeg.damagenexus.api.display.DamageContributionLogFormatter;
import io.github.naimjeg.damagenexus.api.display.DamageContributionSummary;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.core.DamageOperation;
import io.github.naimjeg.damagenexus.core.rule.StackingTrace;
import io.github.naimjeg.damagenexus.core.trace.DamageMutationType;
import io.github.naimjeg.damagenexus.core.trace.RuleSkipReason;

import java.util.List;
import java.util.Locale;

public final class Slf4jCombatTrace implements CombatTrace {

    private final CombatTraceState state;
    private final CombatTransactionLog transaction;
    private final CombatPipelineLog pipeline;
    private final CombatRuleLog rules;
    private final CombatMutationLog mutations;
    private final CombatCalculationLog calculation;
    private final CombatContributionLog contributions;

    Slf4jCombatTrace(long damageId) {
        this.state = new CombatTraceState(damageId);
        this.transaction = new TransactionLog(state);
        this.pipeline = new PipelineLog(state);
        this.rules = new RuleLog(state);
        this.mutations = new MutationLog(state);
        this.calculation = new CalculationLog(state);
        this.contributions = new ContributionLog(state);
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public CombatTransactionLog transaction() {
        return transaction;
    }

    @Override
    public CombatPipelineLog pipeline() {
        return pipeline;
    }

    @Override
    public CombatRuleLog rules() {
        return rules;
    }

    @Override
    public CombatMutationLog mutations() {
        return mutations;
    }

    @Override
    public CombatCalculationLog calculation() {
        return calculation;
    }

    @Override
    public CombatContributionLog contributions() {
        return contributions;
    }

    @Override
    public int mutationCheckpoint() {
        return state.operationCount();
    }

    @Override
    public void rollbackMutations(int checkpoint) {
        state.rollbackOperations(checkpoint);
    }

    private record ContributionLog(
            CombatTraceState state
    ) implements CombatContributionLog {

        @Override
        public void noOp(
                DamageContributionDescriptor descriptor
        ) {
            entry("CONTRIB~", descriptor);
        }

        @Override
        public void noOpSummary(
                DamageContributionSummary summary
        ) {
            summaryEntry("CONTRIB_SUMMARY~", summary);
        }

        @Override
        public void summary(
                int appliedCount,
                int rejectedCount,
                int noOpCount
        ) {
            state.info(
                    DamageNexusLogKind.TRACE_SUMMARY,
                    "{} CONTRIBUTIONS applied={} rejected={} noop={}",
                    state.prefix(),
                    appliedCount,
                    rejectedCount,
                    noOpCount
            );
        }

        @Override
        public void applied(
                DamageContributionDescriptor descriptor
        ) {
            entry("CONTRIB+", descriptor);
        }

        @Override
        public void rejected(
                DamageContributionDescriptor descriptor
        ) {
            entry("CONTRIB-", descriptor);
        }

        private void entry(
                String marker,
                DamageContributionDescriptor descriptor
        ) {
            if (descriptor == null) {
                return;
            }

            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    DamageNexusLogKind.TRACE_DETAIL,
                    "{} {} id={} status={} source={} op={} phase={} channel={} bucket={} pre_bucket={} value={} group={} subgroup={} trace={}",
                    state.prefix(),
                    marker,
                    descriptor.id(),
                    descriptor.status(),
                    descriptor.sourceKind(),
                    descriptor.operationKind(),
                    descriptor.phase(),
                    descriptor.channel()
                            .map(Object::toString)
                            .orElse("-"),
                    descriptor.applicationBucket()
                            .map(Object::toString)
                            .orElse("-"),
                    descriptor.preMultiplierBucket()
                            .map(Object::toString)
                            .orElse("-"),
                    CombatTraceState.fmt(descriptor.value()),
                    descriptor.displayGroup()
                            .map(Object::toString)
                            .orElse("-"),
                    descriptor.displaySubgroup()
                            .map(Object::toString)
                            .orElse("-"),
                    descriptor.traceLabel()
                            .orElse("-")
            );
        }

        @Override
        public void appliedSummary(DamageContributionSummary summary) {
            summaryEntry("CONTRIB_SUMMARY+", summary);
        }

        @Override
        public void rejectedSummary(DamageContributionSummary summary) {
            summaryEntry("CONTRIB_SUMMARY-", summary);
        }

        private void summaryEntry(
                String marker,
                DamageContributionSummary summary
        ) {
            if (summary == null) {
                return;
            }

            if (!state.accepts(DamageNexusLogKind.TRACE_SUMMARY)) {
                return;
            }

            state.info(
                    DamageNexusLogKind.TRACE_SUMMARY,
                    "{} {} text=\"{}\" status={} source={} op={} phase={} channel={} bucket={} pre_bucket={} count={} total={} name={} trace={}",
                    state.prefix(),
                    marker,
                    DamageContributionLogFormatter.compact(summary),
                    summary.status(),
                    summary.sourceKind(),
                    summary.operationKind(),
                    summary.phase(),
                    summary.channel().map(Object::toString).orElse("-"),
                    summary.applicationBucket().map(Object::toString).orElse("-"),
                    summary.preMultiplierBucket().map(Object::toString).orElse("-"),
                    summary.count(),
                    CombatTraceState.fmt(summary.totalValue()),
                    summary.displayName().orElse("-"),
                    summary.traceLabel().orElse("-")
            );
        }
    }

    private record TransactionLog(CombatTraceState state) implements CombatTransactionLog {

        @Override
        public void begin(
                String attackerName,
                String victimName,
                String sourceId,
                String initialChannel,
                float eventOriginalAmount,
                float initialBaseAmount
        ) {
            state.info(
                    DamageNexusLogKind.TRACE_SUMMARY,
                    "{} BEGIN attacker={} victim={} source={} channel={} event_original={} initial_base={}",
                    state.prefix(),
                    attackerName,
                    victimName,
                    sourceId,
                    initialChannel,
                    CombatTraceState.fmt(eventOriginalAmount),
                    CombatTraceState.fmt(initialBaseAmount)
            );
        }

        @Override
        public void preNexus(float amount) {
            state.info(
                    "{} PRE original={}",
                    state.prefix(),
                    CombatTraceState.fmt(amount)
            );
        }

        @Override
        public void apply(
                float eventOriginalAmount,
                float initialBaseAmount,
                float offensiveTotal,
                float finalEventAmount
        ) {
            state.info(
                    DamageNexusLogKind.TRACE_SUMMARY,
                    "{} APPLY event_original={} initial_base={} offensive_total={} final_event_amount={}",
                    state.prefix(),
                    CombatTraceState.fmt(eventOriginalAmount),
                    CombatTraceState.fmt(initialBaseAmount),
                    CombatTraceState.fmt(offensiveTotal),
                    CombatTraceState.fmt(finalEventAmount)
            );
        }

        @Override
        public void end() {
            state.info("{} END", state.prefix());
        }
    }

    private record PipelineLog(CombatTraceState state) implements CombatPipelineLog {

        @Override
        public void layout(DamagePhase phase, List<DamagePhaseProcessor> processors) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info("[DamageNexus] Pipeline phase {}:", phase);

            for (DamagePhaseProcessor processor : processors) {
                state.info(
                        "[DamageNexus]   processor={} priority={}",
                        processor.getClass().getName(),
                        processor.getPriority()
                );
            }
        }

        @Override
        public void phase(DamagePhase phase) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info("{} PHASE {}", state.prefix(), phase);
        }

        @Override
        public void processorRun(
                DamagePhase phase,
                DamagePhaseProcessor processor
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{} [{}] PROCESSOR_RUN processor={} priority={}",
                    state.prefix(),
                    phase,
                    processor.getClass().getSimpleName(),
                    processor.getPriority()
            );
        }

        @Override
        public void processorSkip(
                DamagePhase phase,
                DamagePhaseProcessor processor
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{} [{}] PROCESSOR_SKIP processor={} priority={} reason=CAN_HANDLE_FALSE",
                    state.prefix(),
                    phase,
                    processor.getClass().getSimpleName(),
                    processor.getPriority()
            );
        }
    }

    private record RuleLog(CombatTraceState state) implements CombatRuleLog {

        @Override
        public void collected(
                DamagePhase phase,
                DamageRuleDefinition rule,
                RuleExecutionContext exec
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{} [{}] RULE_COLLECT rule={} provider={} role={} slot={}",
                    state.prefix(),
                    phase,
                    rule.id(),
                    exec.providerType(),
                    exec.role(),
                    exec.equipmentSlot()
            );
        }

        @Override
        public void skipped(
                DamagePhase phase,
                DamageRuleDefinition rule,
                RuleSkipReason reason
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{} [{}] RULE_SKIP rule={} reason={}",
                    state.prefix(),
                    phase,
                    rule.id(),
                    reason
            );
        }

        @Override
        public void phaseMismatch(
                DamagePhase runningPhase,
                DamageRuleDefinition rule
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{} [{}] RULE_SKIP rule={} reason={} rule_phase={}",
                    state.prefix(),
                    runningPhase,
                    rule.id(),
                    RuleSkipReason.PHASE_MISMATCH,
                    rule.phase()
            );
        }

        @Override
        public void roleMismatch(
                DamagePhase phase,
                DamageRuleDefinition rule,
                RuleExecutionContext exec
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{} [{}] RULE_SKIP rule={} reason={} rule_role={} runtime_role={} provider={}",
                    state.prefix(),
                    phase,
                    rule.id(),
                    RuleSkipReason.ROLE_MISMATCH,
                    rule.role(),
                    exec.role(),
                    exec.providerType()
            );
        }

        @Override
        public void conditionFailed(
                DamagePhase phase,
                DamageRuleDefinition rule,
                DamageRuleCondition condition
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{} [{}] RULE_SKIP rule={} reason={} condition={}",
                    state.prefix(),
                    phase,
                    rule.id(),
                    RuleSkipReason.CONDITION_FAILED,
                    condition.type()
            );
        }

        @Override
        public void executed(
                DamagePhase phase,
                DamageRuleDefinition rule
        ) {
            state.info(
                    "{} [{}] RULE_EXECUTE rule={} trace_name={}",
                    state.prefix(),
                    phase,
                    rule.id(),
                    rule.traceName()
            );
        }

        @Override
        public void result(
                DamagePhase phase,
                DamageRuleDefinition rule,
                int appliedOperations,
                int rejectedOperations,
                int noOpOperations
        ) {
            state.info(
                    "{} [{}] RULE_RESULT rule={} applied={} rejected={} noop={}",
                    state.prefix(),
                    phase,
                    rule.id(),
                    appliedOperations,
                    rejectedOperations,
                    noOpOperations
            );
        }

        @Override
        public void stackingDrop(StackingTrace trace) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{} [{}] STACKING_DROP policy={} kept={} kept_value={} dropped={} dropped_value={}",
                    state.prefix(),
                    trace.phase(),
                    trace.policy(),
                    trace.kept(),
                    CombatTraceState.fmt(trace.keptValue()),
                    trace.dropped(),
                    CombatTraceState.fmt(trace.droppedValue())
            );
        }
    }

    private record MutationLog(CombatTraceState state) implements CombatMutationLog {

        @Override
        public void baseDamage(
                String sourceId,
                DamagePhase phase,
                DamageApplicationBucket applicationBucket,
                float value
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.addOperation(DamageOperation.baseDamage(
                    sourceId,
                    phase,
                    applicationBucket,
                    value
            ));
        }

        @Override
        public void applicationPreMultiplier(
                String sourceId,
                DamagePhase phase,
                DamageApplicationBucket applicationBucket,
                int preMultiplierBucket,
                float value
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.addOperation(DamageOperation.applicationPreMultiplier(
                    sourceId,
                    phase,
                    applicationBucket,
                    preMultiplierBucket,
                    value
            ));
        }

        @Override
        public void mutation(
                String sourceId,
                DamagePhase phase,
                DamageMutationType type,
                float value
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.addOperation(new DamageOperation(
                    sourceId,
                    phase,
                    type,
                    value
            ));
        }

        @Override
        public void rejected(
                String action,
                DamagePhase currentPhase,
                String reason
        ) {
            state.info(
                    DamageNexusLogKind.TRACE_DETAIL,
                    "{} MUTATION_REJECTED action={} phase={} reason={}",
                    state.prefix(),
                    action,
                    currentPhase,
                    reason
            );
        }
    }

    private record CalculationLog(CombatTraceState state) implements CombatCalculationLog {

        @Override
        public void offenseStart() {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info("{} OFFENSE", state.prefix());

            if (!state.hasOperations()) {
                return;
            }

            for (DamageOperation op : state.operations()) {
                if (!CombatTraceState.isOffensiveOperation(op)) {
                    continue;
                }

                state.info(
                        "{}   mutation [{}] type={} source={}{} value={}",
                        state.prefix(),
                        op.phase(),
                        op.type(),
                        op.source(),
                        CombatTraceState.formatMutationMetadata(op),
                        CombatTraceState.fmt(op.value())
                );
            }
        }

        @Override
        public void channelResult(
                String channelId,
                float baseAmount,
                float result
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{}   channel={} base={} offensive={}",
                    state.prefix(),
                    channelId,
                    CombatTraceState.fmt(baseAmount),
                    CombatTraceState.fmt(result)
            );
        }

        @Override
        public void offensiveSummary(float total) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{}   offensive_total={}",
                    state.prefix(),
                    CombatTraceState.fmt(total)
            );
        }

        @Override
        public void armor(
                String channelId,
                float damageBefore,
                float baseArmor,
                float armorEffectiveness,
                float effectiveArmor,
                float reductionPercent
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{}   armor channel={} damage_before={} base_armor={} armor_eff={} effective_formula_armor={} reduction={}",
                    state.prefix(),
                    channelId,
                    CombatTraceState.fmt(damageBefore),
                    CombatTraceState.fmt(baseArmor),
                    CombatTraceState.fmt(armorEffectiveness),
                    CombatTraceState.fmt(effectiveArmor),
                    CombatTraceState.pct(reductionPercent)
            );
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
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{}   resistance channel={} channel_rating={} temp_rating={} category_rating={} total_rating={} reduction={}",
                    state.prefix(),
                    channelId,
                    CombatTraceState.fmt(channelRating),
                    CombatTraceState.fmt(temporaryRating),
                    CombatTraceState.fmt(categoryRating),
                    CombatTraceState.fmt(totalRating),
                    CombatTraceState.pct(reductionPercent)
            );
        }

        @Override
        public void attributeScaling(
                String scope,
                String target,
                String attributeId,
                String bucketId,
                float additive
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }
            state.info(
                    "{}   attribute_scaling scope={} target={} attribute={} bucket={} additive={}",
                    state.prefix(), scope, target, attributeId, bucketId,
                    CombatTraceState.fmt(additive)
            );
        }

        @Override
        public void enchantmentProtection(
                String enchantmentId,
                int level,
                float scoreDelta,
                float ratingDelta
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{}   enchant_protection enchant={} score_delta={} temp_rating_delta={}",
                    state.prefix(),
                    enchantmentId,
                    CombatTraceState.fmt(scoreDelta),
                    CombatTraceState.fmt(ratingDelta)
            );
        }

        @Override
        public void defensiveSummary(float total) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info("{} DEFENSE", state.prefix());

            for (DamageOperation op : state.operations()) {
                if (!CombatTraceState.isDefensiveOperation(op)) {
                    continue;
                }

                state.info(
                        "{}   mutation [{}] type={} source={}{} value={}",
                        state.prefix(),
                        op.phase(),
                        op.type(),
                        op.source(),
                        CombatTraceState.formatMutationMetadata(op),
                        CombatTraceState.fmt(op.value())
                );
            }

            state.info(
                    "{}   final_after_mitigation={}",
                    state.prefix(),
                    CombatTraceState.fmt(total)
            );
        }

        @Override
        public void bucketResult(
                String channelId,
                DamageApplicationBucket applicationBucket,
                float baseAmount,
                float offensiveAmount,
                float postMitigationAmount,
                boolean affectedByMitigation
        ) {
            if (!state.accepts(DamageNexusLogKind.TRACE_DETAIL)) {
                return;
            }

            state.info(
                    "{}   bucket channel={} application_bucket={} base={} offensive={} post_mitigation={} mitigated={}",
                    state.prefix(),
                    channelId,
                    applicationBucket.name().toLowerCase(Locale.ROOT),
                    CombatTraceState.fmt(baseAmount),
                    CombatTraceState.fmt(offensiveAmount),
                    CombatTraceState.fmt(postMitigationAmount),
                    affectedByMitigation
            );
        }

    }
}

