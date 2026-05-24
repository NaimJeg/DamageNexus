package io.github.naimjeg.damagenexus.api.damage;

import java.util.Objects;

/** A resolver claim containing only the authoritative entity-role assignment. */
public record DamageAttributionResolution(DamageAttribution attribution) {
    public DamageAttributionResolution {
        Objects.requireNonNull(attribution, "attribution");
    }
}
