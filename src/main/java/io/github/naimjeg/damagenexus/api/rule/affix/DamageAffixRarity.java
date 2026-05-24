package io.github.naimjeg.damagenexus.api.rule.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum DamageAffixRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    UNIQUE;

    public static final Codec<DamageAffixRarity> CODEC =
            Codec.STRING.comapFlatMap(
                    DamageAffixRarity::decode,
                    DamageAffixRarity::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<DamageAffixRarity> decode(String name) {
        for (DamageAffixRarity value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage affix rarity: " + name);
    }
}
