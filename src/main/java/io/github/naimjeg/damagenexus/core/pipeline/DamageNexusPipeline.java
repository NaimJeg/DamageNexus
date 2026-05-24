package io.github.naimjeg.damagenexus.core.pipeline;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.core.util.StrictCallbackFailure;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLifecycleLog;
import io.github.naimjeg.damagenexus.diagnostics.logging.DiagnosticTextSanitizer;
import io.github.naimjeg.damagenexus.registry.DamagePhaseProcessorRegistry;
import io.github.naimjeg.damagenexus.registry.ModDamageProcessors;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DamageNexusPipeline {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicReference<PipelineSnapshot> CURRENT_SNAPSHOT =
            new AtomicReference<>(PipelineSnapshot.empty());

    private static final Comparator<PipelineEntry> ENTRY_ORDERING =
            Comparator
                    .comparingInt(PipelineEntry::priority)
                    .reversed()
                    .thenComparingInt(entry -> entry.external() ? 1 : 0)
                    .thenComparing(entry -> entry.registryId() == null
                            ? ""
                            : entry.registryId().toString())
                    .thenComparingLong(PipelineEntry::insertionOrder);

    private static PipelineSnapshot snapshot() {
        int externalVersion = DamagePhaseProcessorRegistry.version();

        while (true) {
            PipelineSnapshot current = CURRENT_SNAPSHOT.get();

            if (current.externalVersion() == externalVersion) {
                return current;
            }

            PipelineSnapshot rebuilt =
                    buildPipelineSnapshot(externalVersion);

            if (CURRENT_SNAPSHOT.compareAndSet(current, rebuilt)) {
                logDuplicatePriorities(rebuilt);

                if (DamageNexusSettings.fullTraceEnabled()) {
                    logPipelineLayout(rebuilt);
                }

                return rebuilt;
            }
        }
    }

    private static PipelineSnapshot buildPipelineSnapshot(int externalVersion) {
        return buildPipelineSnapshot(
                internalProcessors(),
                DamagePhaseProcessorRegistry.externalProcessors(),
                externalVersion,
                true
        );
    }

    static PipelineSnapshot buildPipelineSnapshot(
            Collection<? extends DamagePhaseProcessor> internalProcessors,
            Collection<? extends DamagePhaseProcessor> externalProcessors,
            int externalVersion
    ) {
        return buildPipelineSnapshot(
                internalProcessors,
                externalProcessors,
                externalVersion,
                false
        );
    }

    private static PipelineSnapshot buildPipelineSnapshot(
            Collection<? extends DamagePhaseProcessor> internalProcessors,
            Collection<? extends DamagePhaseProcessor> externalProcessors,
            int externalVersion,
            boolean resolveRegistryIds
    ) {
        Map<DamagePhase, List<PipelineEntry>> phaseProcessors =
                new EnumMap<>(DamagePhase.class);

        for (DamagePhase phase : DamagePhase.values()) {
            phaseProcessors.put(phase, new ArrayList<>());
        }

        long insertionOrder = 0L;

        for (DamagePhaseProcessor processor : internalProcessors) {
            insertionOrder = addProcessor(
                    phaseProcessors,
                    processor,
                    false,
                    resolveRegistryIds ? processorRegistryId(processor) : null,
                    insertionOrder
            );
        }

        for (DamagePhaseProcessor processor : externalProcessors) {
            insertionOrder = addProcessor(
                    phaseProcessors,
                    processor,
                    true,
                    null,
                    insertionOrder
            );
        }

        Map<DamagePhase, List<PipelineEntry>> frozen =
                new EnumMap<>(DamagePhase.class);

        for (DamagePhase phase : DamagePhase.values()) {
            List<PipelineEntry> list = phaseProcessors.get(phase);
            list.sort(ENTRY_ORDERING);
            frozen.put(phase, List.copyOf(list));
        }

        return new PipelineSnapshot(
                Collections.unmodifiableMap(frozen),
                externalVersion
        );
    }

    private static List<DamagePhaseProcessor> internalProcessors() {
        return ModDamageProcessors.builtInProcessors();
    }

    private static long addProcessor(
            Map<DamagePhase, List<PipelineEntry>> phaseProcessors,
            DamagePhaseProcessor processor,
            boolean external,
            Identifier registryId,
            long insertionOrder
    ) {
        if (processor == null) {
            return insertionOrder;
        }

        DamagePhase phase;

        try {
            phase = processor.phase();
        } catch (Exception exception) {
            handleProcessorFailure(
                    "getPhase",
                    processor,
                    external,
                    exception
            );
            return insertionOrder;
        }

        if (phase == null) {
            LOGGER.error(
                    "[DamageNexus] {} processor {} returned null phase",
                    external ? "External" : "Internal",
                    processor.getClass().getName()
            );
            return insertionOrder;
        }

        List<PipelineEntry> list = phaseProcessors.get(phase);

        if (list == null) {
            LOGGER.error(
                    "[DamageNexus] {} processor {} returned invalid phase {}",
                    external ? "External" : "Internal",
                    processor.getClass().getName(),
                    phase
            );
            return insertionOrder;
        }

        list.add(new PipelineEntry(
                processor,
                external,
                registryId,
                insertionOrder,
                safePriority(processor, external)
        ));

        return insertionOrder + 1L;
    }

    private static int safePriority(
            DamagePhaseProcessor processor,
            boolean external
    ) {
        try {
            return processor.getPriority();
        } catch (Exception exception) {
            handleProcessorFailure(
                    "getPriority",
                    processor,
                    external,
                    exception
            );

            return Integer.MIN_VALUE;
        }
    }

    private static Identifier processorRegistryId(DamagePhaseProcessor processor) {
        try {
            return ModDamageProcessors.builtInProcessorId(processor);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void logPipelineLayout(PipelineSnapshot snapshot) {
        for (DamagePhase phase : DamagePhase.values()) {
            DamageNexusLifecycleLog.pipelinePhase(phase);

            for (PipelineEntry entry : snapshot.processors(phase)) {
                DamageNexusLifecycleLog.pipelineProcessor(
                        entry.processor().getClass().getSimpleName(),
                        entry.priority(),
                        entry.kind()
                );
            }
        }
    }

    private static void logDuplicatePriorities(PipelineSnapshot snapshot) {
        for (DamagePhase phase : DamagePhase.values()) {
            Map<Integer, List<PipelineEntry>> byPriority =
                    new HashMap<>();

            for (PipelineEntry entry : snapshot.processors(phase)) {
                byPriority
                        .computeIfAbsent(entry.priority(), ignored -> new ArrayList<>())
                        .add(entry);
            }

            for (Map.Entry<Integer, List<PipelineEntry>> entry : byPriority.entrySet()) {
                if (entry.getValue().size() < 2) {
                    continue;
                }

                String processors = entry.getValue().stream()
                        .map(PipelineEntry::diagnosticName)
                        .collect(Collectors.joining(", "));

                String signature = phase + "/" + entry.getKey() + "/" + processors;
                if (!DamageNexusDiagnosticState.shouldLog(
                        DamageNexusDiagnosticState.Domain.PIPELINE_LAYOUT,
                        signature,
                        "deterministic_tie",
                        "source_kind_registry_id_insertion_order"
                )) {
                    continue;
                }

                LOGGER.debug(
                        "[DamageNexus] Multiple damage phase processors share phase={} priority={}: {}. "
                                + "Ordering will use source kind, registry id, and insertion order.",
                        phase,
                        entry.getKey(),
                        processors
                );
            }
        }
    }

    public static void clearCache() {
        CURRENT_SNAPSHOT.set(PipelineSnapshot.empty());
        DamageNexusDiagnosticState.clearAll();
    }

    public static DamageExecutionSummary execute(DamageNexusContext ctx) {
        executePipeline(ctx);
        return ctx.executionSummary();
    }

    private static void executePipeline(DamageNexusContext ctx) {
        PipelineSnapshot snapshot = snapshot();

        if (!ctx.isManaged()) {
            return;
        }

        runPhase(snapshot, DamagePhase.BASE_MODIFICATION, ctx);
        if (finishIfCancelled(ctx)) return;

        runPhase(snapshot, DamagePhase.TYPE_SCALING, ctx);
        if (finishIfCancelled(ctx)) return;

        // Private Phase 8 handoff: all converted/new channels are now final.
        ctx.setCurrentProcessingPhase(DamagePhase.TYPE_SCALING);
        DamageAttributeScalingEngine.execute(ctx);

        // Private Phase 7 pre-pass: providers, freeze, one roll, one effect.
        // Ordinary public CRITICAL_HIT processors/rules always run afterward.
        ctx.setCurrentProcessingPhase(DamagePhase.CRITICAL_HIT);
        CriticalDecisionEngine.execute(ctx);
        runPhase(snapshot, DamagePhase.CRITICAL_HIT, ctx);
        if (finishIfCancelled(ctx)) return;

        runPhase(snapshot, DamagePhase.CONDITIONAL_MULTI, ctx);
        if (finishIfCancelled(ctx)) return;

        runPhase(snapshot, DamagePhase.GLOBAL_ADJUSTMENT, ctx);
        if (finishIfCancelled(ctx)) return;

        ctx.finalizeOffensiveDamage();

        runPhase(snapshot, DamagePhase.MITIGATION_SETUP, ctx);
        if (finishIfCancelled(ctx)) return;

        ctx.calculateDefensiveDamage();

        runPhase(snapshot, DamagePhase.FINAL_OVERRIDE, ctx);
        if (finishIfCancelled(ctx)) return;

        ctx.applyIncomingDamageToEvent();
    }

    private static boolean finishIfCancelled(DamageNexusContext ctx) {
        if (!ctx.isDamageCancelled()) {
            return false;
        }

        ctx.applyCancelledDamageToEvent();
        return true;
    }

    private static void runPhase(
            PipelineSnapshot snapshot,
            DamagePhase phase,
            DamageNexusContext ctx
    ) {
        ctx.setCurrentProcessingPhase(phase);
        ctx.trace().pipeline().phase(phase);

        List<PipelineEntry> processors = snapshot.processors(phase);

        if (processors == null || processors.isEmpty()) {
            return;
        }

        for (PipelineEntry entry : processors) {
            DamagePhaseProcessor processor = entry.processor();

            if (!safeCanHandle(entry, phase, ctx)) {
                ctx.trace().pipeline().processorSkip(
                        phase,
                        processor
                );
                continue;
            }

            ctx.trace().pipeline().processorRun(
                    phase,
                    processor
            );

            safeApply(entry, phase, ctx);

            if (ctx.isDamageCancelled()) {
                return;
            }
        }
    }

    private static boolean safeCanHandle(
            PipelineEntry entry,
            DamagePhase phase,
        DamageNexusContext ctx
    ) {
        DamageNexusContext.MutableStateCheckpoint checkpoint =
                ctx.checkpointMutableState();
        try {
            return entry.processor().canHandle(
                    contextForProcessorCallback(entry, ctx)
            );
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);
            if (exception instanceof StrictCallbackFailure strictFailure) {
                throw strictFailure;
            }
            handleProcessorFailure(
                    "canHandle/" + phase,
                    entry.processor(),
                    entry.external(),
                    exception
            );

            return false;
        }
    }

    private static void safeApply(
            PipelineEntry entry,
            DamagePhase phase,
        DamageNexusContext ctx
    ) {
        DamageNexusContext.MutableStateCheckpoint checkpoint =
                ctx.checkpointMutableState();
        try {
            entry.processor().apply(
                    contextForProcessorCallback(entry, ctx)
            );
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);
            if (exception instanceof StrictCallbackFailure strictFailure) {
                throw strictFailure;
            }
            handleProcessorFailure(
                    "apply/" + phase,
                    entry.processor(),
                    entry.external(),
                    exception
            );
        }
    }

    static DamageRuleContext contextForProcessorCallback(
            PipelineEntry entry,
            DamageRuleContext context
    ) {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(context, "context");

        return entry.external()
                ? DamageRuleContextViews.restricted(context)
                : context;
    }

    private static void handleProcessorFailure(
            String stage,
            DamagePhaseProcessor processor,
            boolean external,
            Exception exception
    ) {
        if (DamageNexusSettings.strictProcessorErrors()) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new IllegalStateException(
                    "[DamageNexus] Processor failure at " + stage
                            + ": " + processor.getClass().getName(),
                    exception
            );
        }

        String processorName = processor == null
                ? "<null>"
                : processor.getClass().getName();

        if (DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.PROCESSOR,
                processorName,
                stage,
                exception.getClass().getName()
        )) {
            LOGGER.error(
                    "[DamageNexus] {} processor failed at stage={} processor={}. "
                            + "The processor will be skipped for this call. "
                            + "Set strictProcessorErrors=true to fail fast.",
                    external ? "External" : "Internal",
                    DiagnosticTextSanitizer.sanitizeLine(stage),
                    DiagnosticTextSanitizer.sanitizeLine(processorName),
                    exception
            );
        }
    }

    record PipelineSnapshot(
            Map<DamagePhase, List<PipelineEntry>> phaseProcessors,
            int externalVersion
    ) {
        static PipelineSnapshot empty() {
            Map<DamagePhase, List<PipelineEntry>> phaseProcessors =
                    new EnumMap<>(DamagePhase.class);

            for (DamagePhase phase : DamagePhase.values()) {
                phaseProcessors.put(phase, List.of());
            }

            return new PipelineSnapshot(
                    Collections.unmodifiableMap(phaseProcessors),
                    -1
            );
        }

        List<PipelineEntry> processors(DamagePhase phase) {
            return phaseProcessors.getOrDefault(phase, List.of());
        }
    }

    record PipelineEntry(
            DamagePhaseProcessor processor,
            boolean external,
            Identifier registryId,
            long insertionOrder,
            int priority
    ) {
        String name() {
            return processor.getClass().getName();
        }

        String kind() {
            return external ? "external" : "internal";
        }

        String diagnosticName() {
            if (registryId != null) {
                return registryId.toString();
            }

            return name() + "(" + kind() + ")";
        }
    }
}

