package io.github.naimjeg.damagenexus.diagnostics.logging;

import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;

public final class CombatTraceFactory {

    private CombatTraceFactory() {
    }

    public static CombatTrace create(
            long damageId
    ) {
        if (!DamageNexusSettings.summaryTraceEnabled()) {
            return NoOpCombatTrace.INSTANCE;
        }

        return new Slf4jCombatTrace(damageId);
    }
}

