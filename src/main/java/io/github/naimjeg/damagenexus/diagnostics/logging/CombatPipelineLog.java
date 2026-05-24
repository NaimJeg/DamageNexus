package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;

import java.util.List;

public interface CombatPipelineLog {

    void layout(DamagePhase phase, List<DamagePhaseProcessor> processors);

    void phase(DamagePhase phase);

    void processorRun(
            DamagePhase phase,
            DamagePhaseProcessor processor
    );

    void processorSkip(
            DamagePhase phase,
            DamagePhaseProcessor processor
    );
}

