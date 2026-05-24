package io.github.naimjeg.damagenexus.api.enums;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import net.minecraft.resources.Identifier;

/**
 * Damage channel value. Built-in {@code *_ID} fields are stable persistent IDs;
 * {@link #index()} is assigned at runtime and must never be serialized.
 */
public record DamageChannel(Identifier id, int index) {

    public static final Identifier UNTYPED_ID =
            DamageNexusIds.id("untyped");

    public static final Identifier PHYSICAL_ID =
            DamageNexusIds.id("physical");

    public static final Identifier FIRE_ID =
            DamageNexusIds.id("fire");

    public static final Identifier COLD_ID =
            DamageNexusIds.id("cold");

    public static final Identifier LIGHTNING_ID =
            DamageNexusIds.id("lightning");

    public static final Identifier MAGIC_ID =
            DamageNexusIds.id("magic");

    public static final Identifier WITHER_ID =
            DamageNexusIds.id("wither");

    public static final Identifier KINETIC_ID =
            DamageNexusIds.id("kinetic");

    public static final Identifier POISON_ID =
            DamageNexusIds.id("poison");

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DamageChannel other)) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
