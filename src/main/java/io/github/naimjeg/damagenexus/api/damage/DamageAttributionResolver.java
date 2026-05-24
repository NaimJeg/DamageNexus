package io.github.naimjeg.damagenexus.api.damage;

import java.util.Optional;

/**
 * Resolves proxy, summon, or area-effect attribution from authoritative
 * server state. Implementations must be side-effect free and must independently
 * verify any owner relationship they claim.
 *
 * <p>Mods loaded in the same JVM are trusted Java code. This API prevents
 * ordinary request-field spoofing and accidental wiring errors; it is not a
 * sandbox against malicious reflection or bytecode modification.</p>
 */
@FunctionalInterface
public interface DamageAttributionResolver {

    /** Returns a claim, or empty when this resolver does not own the source. */
    Optional<DamageAttributionResolution> resolve(
            DamageAttributionQuery query
    );
}
