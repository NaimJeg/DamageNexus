package io.github.naimjeg.damagenexus.api.damage;

/** Identifies how the framework established the authoritative attribution. */
public enum DamageAttributionSource {
    /** Safe roles inferred from a native Minecraft {@code DamageSource}. */
    VANILLA_DEFAULT,
    /** Roles declared by a public request and accepted without an adapter. */
    PUBLIC_REQUEST,
    /** Roles claimed by a registered server-side attribution resolver. */
    REGISTERED_RESOLVER
}
