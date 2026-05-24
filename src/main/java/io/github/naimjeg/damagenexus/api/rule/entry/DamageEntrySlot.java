package io.github.naimjeg.damagenexus.api.rule.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum DamageEntrySlot {
    /**
     * Any item-backed source, including equipment and captured projectile items.
     */
    ITEM,

    /**
     * An attacker/victim mainhand or offhand equipment source, or the captured
     * weapon used to create a projectile.
     */
    WEAPON,

    /**
     * An attacker/victim head, chest, legs, or feet equipment source.
     */
    ARMOR,

    /**
     * The captured item source of a projectile attack.
     */
    PROJECTILE;

    public static final Codec<DamageEntrySlot> CODEC =
            Codec.STRING.comapFlatMap(
                    DamageEntrySlot::decode,
                    DamageEntrySlot::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<DamageEntrySlot> decode(String name) {
        for (DamageEntrySlot value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage entry slot: " + name);
    }
}
