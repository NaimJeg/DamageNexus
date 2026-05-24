package io.github.naimjeg.damagenexus.api.rule;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum DamageRuleRole {
    OFFENSIVE,
    DEFENSIVE,
    ANY;

    public static final Codec<DamageRuleRole> CODEC =
            Codec.STRING.xmap(
                    name -> DamageRuleRole.valueOf(name.toUpperCase(Locale.ROOT)),
                    role -> role.name().toLowerCase(Locale.ROOT)
            );

    public boolean canRunAs(DamageRuleRole runtimeRole) {
        return this == ANY || this == runtimeRole;
    }
}
