package io.github.naimjeg.damagenexus.api.rule.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum DamageAffixStacking {
    STACK,
    UNIQUE_AFFIX,
    UNIQUE_GROUP,
    HIGHEST_LEVEL,
    REPLACE;

    public static final Codec<DamageAffixStacking> CODEC =
            Codec.STRING.comapFlatMap(
                    DamageAffixStacking::decode,
                    DamageAffixStacking::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<DamageAffixStacking> decode(String name) {
        for (DamageAffixStacking value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage affix stacking mode: " + name);
    }
}
