package io.github.naimjeg.damagenexus.api.rule.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum DamageAffixSlot {
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

    public static final Codec<DamageAffixSlot> CODEC =
            Codec.STRING.comapFlatMap(
                    DamageAffixSlot::decode,
                    DamageAffixSlot::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<DamageAffixSlot> decode(String name) {
        for (DamageAffixSlot value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage affix slot: " + name);
    }
}
