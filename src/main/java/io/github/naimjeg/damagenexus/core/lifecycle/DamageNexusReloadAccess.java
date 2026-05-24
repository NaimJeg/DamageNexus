package io.github.naimjeg.damagenexus.core.lifecycle;

/**
 * Framework-only capability for constructing registry reload listeners.
 *
 * <p>NeoForge owns reload execution and failure handling. DamageNexus keeps
 * each published registry snapshot atomic instead of exposing a separate,
 * globally mutable RELOADING lifecycle state.</p>
 */
public final class DamageNexusReloadAccess {

    DamageNexusReloadAccess() {
    }

    public void requireFrameworkOwner(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action");
        }
    }
}
