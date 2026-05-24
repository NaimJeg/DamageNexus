package io.github.naimjeg.damagenexus.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.api.damage.DamageOrigin;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.core.request.DamageAdmissionResult;
import org.slf4j.Logger;

/** Rate-limited rejection diagnostics and opt-in full admission tracing. */
public final class DamageAdmissionDiagnosticsLog {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DamageAdmissionDiagnosticsLog() {
    }

    public static void accepted(
            DamageOrigin origin,
            DamageAdmissionResult admission
    ) {
        if (!DamageNexusSettings.fullTraceEnabled()) {
            return;
        }
        LOGGER.info(
                "[DamageNexus] Admission accepted: damageId={}, "
                        + "rootDamageId={}, parentDamageId={}, depth={}, "
                        + "rootCount={}, tickCount={}",
                origin.lineage().damageId(),
                origin.lineage().rootDamageId(),
                origin.lineage().parentDamageId(),
                origin.lineage().recursionDepth(),
                admission.rootDerivedCount(),
                admission.serverTickCount()
        );
    }

    public static void nativeRejected(
            DamageOrigin origin,
            DamageAdmissionResult admission
    ) {
        DamageFailureReason reason = admission.reason();
        if (reason == null || !DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.DAMAGE_ADMISSION,
                origin.source().damageType().identifier().toString(),
                "native_incoming",
                reason.name()
        )) {
            return;
        }

        LOGGER.warn(
                "[DamageNexus] Native managed damage rejected: reason={}, "
                        + "damageId={}, rootDamageId={}, parentDamageId={}, "
                        + "depth={}, rootCount={}, tickCount={}",
                reason,
                origin.lineage().damageId(),
                origin.lineage().rootDamageId(),
                origin.lineage().parentDamageId(),
                origin.lineage().recursionDepth(),
                admission.rootDerivedCount(),
                admission.serverTickCount()
        );
    }
}
