package io.github.naimjeg.damagenexus.api.rule.entry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum DamageEntryStacking {
    STACK,
    UNIQUE_ENTRY,
    UNIQUE_GROUP,
    REPLACE;

    public static final Codec<DamageEntryStacking> CODEC =
            Codec.STRING.comapFlatMap(
                    DamageEntryStacking::decode,
                    DamageEntryStacking::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static DataResult<DamageEntryStacking> decode(String name) {
        for (DamageEntryStacking value : values()) {
            if (value.serializedName().equals(name)) {
                return DataResult.success(value);
            }
        }
        return DataResult.error(() ->
                "Unknown damage entry stacking mode: " + name);
    }
}
