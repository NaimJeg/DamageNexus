package io.github.naimjeg.damagenexus.diagnostics.logging;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Shared bounded diagnostic state for stable, low-cardinality failure keys.
 */
public final class DamageNexusDiagnosticState {

    private static final int CAPACITY_PER_DOMAIN = 1_024;
    private static final Map<Domain, BoundedDiagnosticLimiter<Key>> LIMITERS =
            createLimiters();

    private DamageNexusDiagnosticState() {
    }

    public static boolean shouldLog(
            Domain domain,
            String identity,
            String stage,
            String reason
    ) {
        Key key = new Key(
                DiagnosticTextSanitizer.sanitizeLine(identity, 128),
                DiagnosticTextSanitizer.sanitizeLine(stage, 128),
                DiagnosticTextSanitizer.sanitizeLine(reason, 128)
        );

        return LIMITERS.get(domain).shouldLog(key);
    }

    public static void clearAll() {
        LIMITERS.values().forEach(BoundedDiagnosticLimiter::clear);
        CompatibilityDiagnosticRateLimiter.clear();
    }

    static int domainSize(Domain domain) {
        return LIMITERS.get(domain).size();
    }

    private static Map<Domain, BoundedDiagnosticLimiter<Key>>
    createLimiters() {
        Map<Domain, BoundedDiagnosticLimiter<Key>> limiters =
                new EnumMap<>(Domain.class);

        for (Domain domain : Domain.values()) {
            limiters.put(
                    domain,
                    new BoundedDiagnosticLimiter<>(
                            CAPACITY_PER_DOMAIN,
                            Duration.ofMinutes(10)
                    )
            );
        }

        return Map.copyOf(limiters);
    }

    public enum Domain {
        ENTRY_VALIDATION,
        AFFIX_VALIDATION,
        RULE_VALIDATION,
        STACKING,
        PROVIDER,
        PROCESSOR,
        EVENT_DISPATCH,
        DAMAGE_ADMISSION,
        ITEM_SECURITY,
        ATTRIBUTION_RESOLVER,
        EXTERNAL_ITEM_SOURCE,
        DATAPACK_RELOAD,
        TEMPLATE_RELOAD,
        TEMPLATE_REFERENCE,
        RULE_EXECUTION,
        POST_SETTLEMENT,
        TRANSACTION_CORRELATION,
        PIPELINE_LAYOUT,
        VANILLA_BRIDGE
    }

    private record Key(
            String identity,
            String stage,
            String reason
    ) {
    }
}
