package io.github.naimjeg.damagenexus.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionEntryPoint;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.List;

/** Rate-limited attribution resolver diagnostics. */
public final class DamageAttributionDiagnosticsLog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier VANILLA_DEFAULT =
            Identifier.fromNamespaceAndPath(
                    "damagenexus", "vanilla_default"
            );

    private DamageAttributionDiagnosticsLog() {
    }

    public static void failure(Identifier id,
                               DamageAttributionEntryPoint entry,
                               Throwable throwable) {
        if (shouldLog(id, entry, "exception")) {
            LOGGER.error(
                    "[DamageNexus] Attribution resolver failed; resolver={}, entry={}",
                    id, entry, throwable
            );
        }
    }

    public static void invalid(Identifier id,
                               DamageAttributionEntryPoint entry,
                               String reason) {
        if (shouldLog(id, entry, "invalid:" + reason)) {
            LOGGER.warn(
                    "[DamageNexus] Attribution resolver returned an invalid claim; resolver={}, entry={}, reason={}",
                    id, entry, reason
            );
        }
    }

    public static void ambiguous(Identifier selected,
                                 List<Identifier> claimants,
                                 DamageAttributionEntryPoint entry) {
        if (shouldLog(selected, entry, "ambiguous")) {
            LOGGER.warn(
                    "[DamageNexus] Multiple attribution resolvers claimed one damage source; selected={}, claimants={}, entry={}",
                    selected, claimants, entry
            );
        }
    }

    public static void nativeNormalized(String role, String reason) {
        if (shouldLog(
                VANILLA_DEFAULT,
                DamageAttributionEntryPoint.NATIVE,
                "normalized:" + role + ":" + reason
        )) {
            LOGGER.warn(
                    "[DamageNexus] Ignored an invalid native attribution role; role={}, reason={}",
                    role, reason
            );
        }
    }

    private static boolean shouldLog(Identifier id,
                                     DamageAttributionEntryPoint entry,
                                     String reason) {
        return DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.ATTRIBUTION_RESOLVER,
                id.toString(),
                entry.name(),
                reason
        );
    }
}
