package io.github.naimjeg.damagenexus.builtin.processor;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOrdering;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.api.rule.RuntimeDamageRule;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.core.pipeline.DamageRuleContextViews;
import io.github.naimjeg.damagenexus.core.rule.DamageRuleExecutor;
import io.github.naimjeg.damagenexus.core.rule.DamageRuleStackingResolver;
import io.github.naimjeg.damagenexus.core.rule.DamageRuleStackingResult;
import io.github.naimjeg.damagenexus.core.rule.StackingTrace;
import io.github.naimjeg.damagenexus.core.util.StrictCallbackFailure;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import io.github.naimjeg.damagenexus.diagnostics.logging.DiagnosticTextSanitizer;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleProviders;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RuleExecutionProcessor(DamagePhase phase) implements DamagePhaseProcessor {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean safeSupportsPhase(
            DamageNexusContext ctx,
            DamageRuleProvider provider,
            DamagePhase phase
    ) {
        try {
            return provider.supportsPhase(phase);
        } catch (Exception exception) {
            handleProviderFailure(
                    ctx,
                    provider,
                    phase,
                    "supportsPhase",
                    exception
            );

            return false;
        }
    }

    private static void safeCollect(
            DamageNexusContext ctx,
            DamageRuleContext providerContext,
            DamageRuleProvider provider,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        DamageNexusContext.MutableStateCheckpoint checkpoint =
                ctx.checkpointMutableState();
        List<RuntimeDamageRule> collected = new ArrayList<>();

        try {
            provider.collect(providerContext, phase, collected);
            validateCollectedRules(provider, phase, collected);
            out.addAll(List.copyOf(collected));
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);
            handleProviderFailure(
                    ctx,
                    provider,
                    phase,
                    "collect",
                    exception
            );
        }
    }

    private static void validateCollectedRules(
            DamageRuleProvider provider,
            DamagePhase phase,
            List<RuntimeDamageRule> collected
    ) {
        for (int index = 0; index < collected.size(); index++) {
            RuntimeDamageRule rule = collected.get(index);
            if (rule == null
                    || rule.definition() == null
                    || rule.executionContext() == null) {
                throw new IllegalArgumentException(
                        "Invalid runtime rule from provider="
                                + providerName(provider)
                                + " phase="
                                + phase
                                + " index="
                                + index
                );
            }
        }
    }

    private static void handleProviderFailure(
            DamageNexusContext ctx,
            DamageRuleProvider provider,
            DamagePhase phase,
            String stage,
            Exception exception
    ) {
        if (DamageNexusSettings.strictRuleErrors()) {
            throw new StrictCallbackFailure(
                    "[DamageNexus] Rule provider failure at "
                            + stage
                            + ": provider="
                            + providerName(provider)
                            + " phase="
                            + phase,
                    exception
            );
        }

        String providerName = providerName(provider);
        if (DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.PROVIDER,
                providerName,
                phase + "/" + stage,
                exception.getClass().getName()
        )) {
            LOGGER.error(
                    "[DamageNexus] Rule provider failed. phase={} stage={} provider={}. "
                            + "Provider output for this phase was skipped. "
                            + "Set strictRuleErrors=true to fail fast.",
                    phase,
                    DiagnosticTextSanitizer.sanitizeLine(stage),
                    DiagnosticTextSanitizer.sanitizeLine(providerName),
                    exception
            );
        }

        ctx.trace().mutations().rejected(
                "rule_provider/" + stage,
                phase,
                "provider=" + providerName
                        + " threw "
                        + exception.getClass().getSimpleName()
                        + ": "
                        + exception.getMessage()
        );
    }

    private static String providerName(DamageRuleProvider provider) {
        return provider == null
                ? "<null>"
                : provider.getClass().getName();
    }

    static DamageRuleContext contextForProviderCallback(
            DamageRuleProvider provider,
            DamageRuleContext internalContext
    ) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(internalContext, "internalContext");

        return DamageRuleProviders.isBuiltin(provider)
                ? internalContext
                : DamageRuleContextViews.restricted(internalContext);
    }

    @Override
    public void apply(DamageRuleContext context) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        List<RuntimeDamageRule> rules = new ArrayList<>();

        for (DamageRuleProvider provider : DamageRuleProviders.all()) {
            if (provider == null) {
                continue;
            }

            if (!safeSupportsPhase(ctx, provider, phase)) {
                continue;
            }

            safeCollect(
                    ctx,
                    contextForProviderCallback(provider, ctx),
                    provider,
                    phase,
                    rules
            );
        }

        if (rules.isEmpty()) {
            return;
        }

        DamageRuleStackingResult result =
                DamageRuleStackingResolver.resolve(rules);

        rules = new ArrayList<>(result.rules());

        if (ctx.trace().enabled()) {
            for (StackingTrace trace : result.traces()) {
                ctx.trace().rules().stackingDrop(trace);
            }
        }

        rules.sort((first, second) ->
                DamageRuleOrdering.compareSamePhasePriorityDescending(
                        first.definition(),
                        second.definition()
                ));

        for (RuntimeDamageRule rule : rules) {
            DamageRuleExecutor.execute(
                    ctx,
                    phase,
                    rule
            );

            if (ctx.isDamageCancelled()) {
                return;
            }
        }
    }

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        return ctx.isManaged();
    }

    @Override
    public int getPriority() {
        return switch (phase) {
            case MITIGATION_SETUP -> 1002;
            default -> 500;
        };
    }
}

