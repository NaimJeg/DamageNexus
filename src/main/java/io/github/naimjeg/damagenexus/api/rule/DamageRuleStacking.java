package io.github.naimjeg.damagenexus.api.rule;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum DamageRuleStacking {
    STACK,
    HIGHEST_VALUE,
    LOWEST_VALUE,
    REPLACE,

    /**
     * Keeps only one rule per stacking group, phase, role, and runtime source.
     * <p>
     * Runtime source includes provider type, source location, source item,
     * equipment slot, owner entity id, and source entity id when available.
     * <p>
     * Examples:
     * - two armor pieces in different slots do not collapse into one source;
     * - mainhand and projectile source do not collapse into one source;
     * - datapack/global rules with the same stacking group still collapse because
     * they have no concrete item/entity source.
     */
    UNIQUE_SOURCE;

    public static final Codec<DamageRuleStacking> CODEC =
            Codec.STRING.xmap(
                    name -> DamageRuleStacking.valueOf(name.toUpperCase(Locale.ROOT)),
                    stacking -> stacking.name().toLowerCase(Locale.ROOT)
            );
}
