package io.github.naimjeg.damagenexus.registry;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusLifecycle;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusRegistrationAccess;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLifecycleLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DamagePhaseProcessorRegistry {

    private static final List<DamagePhaseProcessor> EXTERNAL_PROCESSORS =
            new ArrayList<>();

    private static int version = 0;

    private DamagePhaseProcessorRegistry() {
    }

    public static synchronized void registerExternal(
            DamageNexusRegistrationAccess access,
            DamagePhaseProcessor processor
    ) {
        DamageNexusLifecycle.requireRegistering(
                access,
                "DamagePhaseProcessorRegistry.registerExternal"
        );
        Objects.requireNonNull(
                processor,
                "DamagePhaseProcessor cannot be null"
        );

        if (EXTERNAL_PROCESSORS.contains(processor)) {
            DamageNexus.LOGGER.warn(
                    "[DamageNexus] Ignoring duplicate external damage phase processor instance: {}",
                    processor.getClass().getName()
            );
            return;
        }

        EXTERNAL_PROCESSORS.add(processor);
        version++;

        DamageNexusLifecycleLog.externalProcessorRegistered(processor);
    }

    public static synchronized List<DamagePhaseProcessor> externalProcessors() {
        return List.copyOf(EXTERNAL_PROCESSORS);
    }

    public static synchronized int version() {
        return version;
    }
}

