package io.github.naimjeg.damagenexus.api;

import io.github.naimjeg.damagenexus.DamageNexus;
import net.minecraft.resources.Identifier;

/**
 * Central factory for identifiers owned by DamageNexus itself.
 *
 * <p>External mods must preserve their own namespace by constructing their
 * complete {@link Identifier}; this helper must not be used to rewrite external IDs.</p>
 */
public final class DamageNexusIds {

    private DamageNexusIds() {
    }

    /** Creates a {@code damagenexus:*} identifier for a built-in resource only. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DamageNexus.MODID, path);
    }
}
