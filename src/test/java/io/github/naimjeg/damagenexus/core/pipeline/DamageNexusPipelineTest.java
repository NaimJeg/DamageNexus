package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.config.DamageNexusConfig;
import io.github.naimjeg.damagenexus.config.DamageNexusConfigValues;
import io.github.naimjeg.damagenexus.config.DeveloperSettings;
import io.github.naimjeg.damagenexus.registry.ModDamageProcessors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNexusPipelineTest {

    private DamageNexusConfigValues originalConfig;

    @AfterEach
    void restoreConfig() throws Exception {
        if (originalConfig != null) {
            setConfig(originalConfig);
        }
    }

    @Test
    void tiedPriorityOrderingIsDeterministic() {
        TestProcessor internalA = new TestProcessor(DamagePhase.BASE_MODIFICATION, 100);
        TestProcessor internalB = new TestProcessor(DamagePhase.BASE_MODIFICATION, 100);
        TestProcessor externalA = new TestProcessor(DamagePhase.BASE_MODIFICATION, 100);
        TestProcessor externalB = new TestProcessor(DamagePhase.BASE_MODIFICATION, 100);

        DamageNexusPipeline.PipelineSnapshot snapshot =
                DamageNexusPipeline.buildPipelineSnapshot(
                        List.of(internalA, internalB),
                        List.of(externalA, externalB),
                        7
                );

        List<DamageNexusPipeline.PipelineEntry> processors =
                snapshot.processors(DamagePhase.BASE_MODIFICATION);

        assertSame(internalA, processors.get(0).processor());
        assertSame(internalB, processors.get(1).processor());
        assertSame(externalA, processors.get(2).processor());
        assertSame(externalB, processors.get(3).processor());
        assertFalse(processors.get(0).external());
        assertFalse(processors.get(1).external());
        assertTrue(processors.get(2).external());
        assertTrue(processors.get(3).external());
    }

    @Test
    void priorityStillRunsDescendingBeforeTieBreakers() {
        TestProcessor lower = new TestProcessor(DamagePhase.BASE_MODIFICATION, 100);
        TestProcessor higher = new TestProcessor(DamagePhase.BASE_MODIFICATION, 200);

        DamageNexusPipeline.PipelineSnapshot snapshot =
                DamageNexusPipeline.buildPipelineSnapshot(
                        List.of(lower, higher),
                        List.of(),
                        0
                );

        List<DamageNexusPipeline.PipelineEntry> processors =
                snapshot.processors(DamagePhase.BASE_MODIFICATION);

        assertSame(higher, processors.get(0).processor());
        assertSame(lower, processors.get(1).processor());
    }

    @Test
    void snapshotListsAndMapAreImmutable() {
        TestProcessor processor = new TestProcessor(DamagePhase.BASE_MODIFICATION, 100);

        DamageNexusPipeline.PipelineSnapshot snapshot =
                DamageNexusPipeline.buildPipelineSnapshot(
                        List.of(processor),
                        List.of(),
                        0
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.processors(DamagePhase.BASE_MODIFICATION).add(
                        new DamageNexusPipeline.PipelineEntry(
                                processor,
                                false,
                                null,
                                99,
                                100
                        )
                )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.phaseProcessors().put(
                        DamagePhase.FINAL_OVERRIDE,
                        List.of()
                )
        );
    }

    @Test
    void rebuiltSnapshotDoesNotMutatePreviousSnapshot() {
        TestProcessor first = new TestProcessor(DamagePhase.BASE_MODIFICATION, 100);
        TestProcessor second = new TestProcessor(DamagePhase.BASE_MODIFICATION, 100);

        DamageNexusPipeline.PipelineSnapshot before =
                DamageNexusPipeline.buildPipelineSnapshot(
                        List.of(),
                        List.of(first),
                        1
                );

        DamageNexusPipeline.PipelineSnapshot after =
                DamageNexusPipeline.buildPipelineSnapshot(
                        List.of(),
                        List.of(second),
                        2
                );

        assertEquals(1, before.externalVersion());
        assertEquals(2, after.externalVersion());
        assertSame(first, before.processors(DamagePhase.BASE_MODIFICATION).getFirst().processor());
        assertSame(second, after.processors(DamagePhase.BASE_MODIFICATION).getFirst().processor());
    }

    @Test
    void externalProcessorCallbacksReceiveRestrictedContext() {
        DamageRuleContext internal = context();
        AtomicReference<DamageRuleContext> canHandleContext =
                new AtomicReference<>();
        AtomicReference<DamageRuleContext> applyContext =
                new AtomicReference<>();
        DamagePhaseProcessor processor = new DamagePhaseProcessor() {
            @Override
            public boolean canHandle(DamageRuleContext ctx) {
                canHandleContext.set(ctx);
                return true;
            }

            @Override
            public void apply(DamageRuleContext ctx) {
                applyContext.set(ctx);
            }

            @Override
            public DamagePhase phase() {
                return DamagePhase.BASE_MODIFICATION;
            }
        };
        DamageNexusPipeline.PipelineEntry entry =
                new DamageNexusPipeline.PipelineEntry(
                        processor,
                        true,
                        null,
                        0,
                        100
                );

        DamageRuleContext callback =
                DamageNexusPipeline.contextForProcessorCallback(
                        entry,
                        internal
                );
        processor.canHandle(callback);
        processor.apply(callback);

        assertNotSame(internal, callback);
        assertSame(callback, canHandleContext.get());
        assertSame(callback, applyContext.get());
        assertFalse(callback instanceof DamageNexusContext);
        assertThrows(
                IllegalArgumentException.class,
                () -> DamageInternalContexts.require(
                        callback,
                        "external processor test"
                )
        );
    }

    @Test
    void internalProcessorCallbackKeepsInternalContext() {
        DamageRuleContext internal = context();
        DamageNexusPipeline.PipelineEntry entry =
                new DamageNexusPipeline.PipelineEntry(
                        new TestProcessor(
                                DamagePhase.BASE_MODIFICATION,
                                100
                        ),
                        false,
                        null,
                        0,
                        100
                );

        assertSame(
                internal,
                DamageNexusPipeline.contextForProcessorCallback(
                        entry,
                        internal
                )
        );
    }

    @Test
    void tolerantProcessorErrorsIsolateRuntimeExceptions() {
        configureStrictProcessorErrors(false);
        DamagePhaseProcessor throwing = throwingPhase(
                new IllegalStateException("ordinary failure")
        );

        DamageNexusPipeline.PipelineSnapshot snapshot =
                DamageNexusPipeline.buildPipelineSnapshot(
                        List.of(),
                        List.of(throwing),
                        1
                );

        assertTrue(snapshot
                .processors(DamagePhase.BASE_MODIFICATION)
                .isEmpty());
    }

    @Test
    void strictProcessorErrorsRethrowRuntimeExceptions() {
        configureStrictProcessorErrors(true);
        DamagePhaseProcessor throwing = throwingPhase(
                new IllegalStateException("ordinary failure")
        );

        assertThrows(
                IllegalStateException.class,
                () -> DamageNexusPipeline.buildPipelineSnapshot(
                        List.of(),
                        List.of(throwing),
                        1
                )
        );
    }

    @Test
    @SuppressWarnings("removal")
    void seriousJvmErrorsAlwaysEscapeProcessorBoundary() {
        configureStrictProcessorErrors(false);

        assertThrows(
                OutOfMemoryError.class,
                () -> buildWith(throwingPhase(
                        new OutOfMemoryError("synthetic")
                ))
        );
        assertThrows(
                StackOverflowError.class,
                () -> buildWith(throwingPhase(
                        new StackOverflowError("synthetic")
                ))
        );
        assertThrows(
                LinkageError.class,
                () -> buildWith(throwingPhase(
                        new LinkageError("synthetic")
                ))
        );
        assertThrows(
                ThreadDeath.class,
                () -> buildWith(throwingPhase(
                        new ThreadDeath()
                ))
        );

        configureStrictProcessorErrors(true);
        assertThrows(
                OutOfMemoryError.class,
                () -> buildWith(throwingPhase(
                        new OutOfMemoryError("synthetic strict")
                ))
        );
        assertThrows(
                ThreadDeath.class,
                () -> buildWith(throwingPhase(
                        new ThreadDeath()
                ))
        );
        assertThrows(
                LinkageError.class,
                () -> buildWith(throwingPhase(
                        new LinkageError("synthetic strict")
                ))
        );
    }

    @Test
    void neoForgeProcessorRegistryCannotBeUsedAsBuiltinTrustMarker()
            throws Exception {
        assertTrue(Modifier.isPrivate(
                ModDamageProcessors.class
                        .getDeclaredField("PROCESSORS")
                        .getModifiers()
        ));
        assertTrue(Modifier.isPrivate(
                ModDamageProcessors.class
                        .getDeclaredField("PROCESSOR_REGISTRY")
                        .getModifiers()
        ));
        assertTrue(Modifier.isPrivate(
                ModDamageProcessors.class
                        .getDeclaredField("BUILTIN_PROCESSORS")
                        .getModifiers()
        ));
    }

    private static DamageRuleContext context() {
        return (DamageRuleContext) Proxy.newProxyInstance(
                DamageRuleContext.class.getClassLoader(),
                new Class<?>[]{DamageRuleContext.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private static DamagePhaseProcessor throwingPhase(Error error) {
        return new DamagePhaseProcessor() {
            @Override
            public void apply(DamageRuleContext ctx) {
            }

            @Override
            public DamagePhase phase() {
                throw error;
            }
        };
    }

    private static DamagePhaseProcessor throwingPhase(
            RuntimeException exception
    ) {
        return new DamagePhaseProcessor() {
            @Override
            public void apply(DamageRuleContext ctx) {
            }

            @Override
            public DamagePhase phase() {
                throw exception;
            }
        };
    }

    private static void buildWith(DamagePhaseProcessor processor) {
        DamageNexusPipeline.buildPipelineSnapshot(
                List.of(),
                List.of(processor),
                1
        );
    }

    private void configureStrictProcessorErrors(boolean strict) {
        if (originalConfig == null) {
            originalConfig = DamageNexusConfig.current();
        }

        DamageNexusConfigValues current = DamageNexusConfig.current();
        setConfig(new DamageNexusConfigValues(
                new DeveloperSettings(
                        current.developer().testCommandsEnabled(),
                        strict,
                        current.developer().strictRuleErrors()
                ),
                current.diagnostics(),
                current.tooltips(),
                current.formulas(),
                current.vanillaCompatibility(),
                current.safety()
        ));
    }

    private static void setConfig(DamageNexusConfigValues values) {
        try {
            Field field =
                    DamageNexusConfig.class.getDeclaredField("CURRENT");
            field.setAccessible(true);
            field.set(null, values);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to set test config", exception);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }

        if (type == boolean.class) {
            return false;
        }

        if (type == long.class) {
            return 0L;
        }

        if (type == int.class) {
            return 0;
        }

        if (type == float.class) {
            return 0.0f;
        }

        throw new IllegalStateException("Unsupported primitive: " + type);
    }

    private record TestProcessor(
            DamagePhase phase,
            int priority
    ) implements DamagePhaseProcessor {

        @Override
        public void apply(DamageRuleContext ctx) {
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }
}
