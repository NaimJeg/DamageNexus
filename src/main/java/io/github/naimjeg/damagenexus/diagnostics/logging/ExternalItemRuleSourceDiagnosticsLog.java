package io.github.naimjeg.damagenexus.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceDirection;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** Rate-limited external item-source diagnostics. */
public final class ExternalItemRuleSourceDiagnosticsLog {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ExternalItemRuleSourceDiagnosticsLog() {
    }

    public static void failure(Identifier provider,
                               EquippedItemRuleSourceDirection direction,
                               Throwable throwable) {
        if (shouldLog(provider, direction, "exception")) {
            LOGGER.error(
                    "[DamageNexus] External item source failed; provider={}, direction={}",
                    provider, direction, throwable
            );
        }
    }

    public static void duplicate(Identifier provider,
                                 Identifier sourceKey,
                                 EquippedItemRuleSourceDirection direction) {
        if (shouldLog(provider, direction, "duplicate:" + sourceKey)) {
            LOGGER.warn(
                    "[DamageNexus] Duplicate external item source ignored; provider={}, sourceKey={}, direction={}",
                    provider, sourceKey, direction
            );
        }
    }

    private static boolean shouldLog(Identifier provider,
                                     EquippedItemRuleSourceDirection direction,
                                     String reason) {
        return DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.EXTERNAL_ITEM_SOURCE,
                provider.toString(), direction.name(), reason
        );
    }
}
