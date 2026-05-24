package io.github.naimjeg.damagenexus.api.enums;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum DamageApplicationBucket {

    /**
     * ATTACK_DAMAGE + Strength / Weakness.
     * Eats vanilla melee cooldown, vanilla melee crit, generic pre/post multipliers,
     * armor and resistance.
     */
    VANILLA_MELEE_BASE(
            true,
            true,
            false,
            true,
            true,
            true,
            true,
            true
    ),

    /**
     * Sharpness / Smite / Bane delta.
     * Eats vanilla melee cooldown and usually crit according to the current DN table.
     */
    VANILLA_MELEE_ENCHANTMENT(
            true,
            true,
            false,
            true,
            true,
            true,
            true,
            true
    ),

    /**
     * Mace fall bonus, spear speed bonus.
     * Vanilla-specific behavior. By default it remains mitigated.
     */
    VANILLA_WEAPON_SPECIAL(
            true,
            true,
            false,
            true,
            true,
            true,
            true,
            true
    ),

    VANILLA_PROJECTILE_BASE(
            false,
            false,
            false,
            true,
            true,
            true,
            true,
            true
    ),

    VANILLA_PROJECTILE_ENCHANTMENT(
            false,
            false,
            false,
            true,
            true,
            true,
            true,
            true
    ),

    VANILLA_PROJECTILE_CRIT_BONUS(
            false,
            false,
            false,
            false,
            true,
            true,
            true,
            true
    ),

    /**
     * Non-melee, non-projectile vanilla damage:
     * fall, fire tick, magic, explosion, environmental, etc.
     * <p>
     * It does not eat melee cooldown, melee crit, or projectile crit,
     * but remains part of normal DN offensive scaling and mitigation.
     */
    VANILLA_OTHER_BASE(
            false,
            false,
            false,
            true,
            true,
            true,
            true,
            true
    ),

    /**
     * Normal DN flat damage.
     * It remains armor/resistance mitigated by default and participates in generic DN multipliers.
     */
    DN_RULE_BASE(
            false,
            false,
            false,
            true,
            true,
            true,
            true,
            true
    ),

    /**
     * Real damage / post bonus.
     * It does not eat generic channel/global pre multipliers, post multipliers,
     * armor, or resistance. Application-scoped pre multipliers still apply because they are explicit.
     */
    DN_TRUE_DAMAGE(
            false,
            false,
            false,
            true,
            false,
            false,
            false,
            false
    );

    public static final int COUNT = values().length;
    public static final Codec<DamageApplicationBucket> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                for (DamageApplicationBucket bucket : values()) {
                    if (bucket.serializedName().equals(name)) {
                        return DataResult.success(bucket);
                    }
                }
                return DataResult.error(() ->
                        "Unknown DamageNexus application bucket: " + name);
            },
            DamageApplicationBucket::serializedName
    );

    private final boolean affectedByMeleeCooldown;
    private final boolean affectedByMeleeCrit;
    private final boolean affectedByProjectileCrit;
    private final boolean affectedByApplicationPreMultiplier;
    private final boolean affectedByChannelPreMultiplier;
    private final boolean affectedByGlobalPreMultiplier;
    private final boolean affectedByPostMultiplier;
    private final boolean affectedByMitigation;

    DamageApplicationBucket(
            boolean affectedByMeleeCooldown,
            boolean affectedByMeleeCrit,
            boolean affectedByProjectileCrit,
            boolean affectedByApplicationPreMultiplier,
            boolean affectedByChannelPreMultiplier,
            boolean affectedByGlobalPreMultiplier,
            boolean affectedByPostMultiplier,
            boolean affectedByMitigation
    ) {
        this.affectedByMeleeCooldown = affectedByMeleeCooldown;
        this.affectedByMeleeCrit = affectedByMeleeCrit;
        this.affectedByProjectileCrit = affectedByProjectileCrit;
        this.affectedByApplicationPreMultiplier = affectedByApplicationPreMultiplier;
        this.affectedByChannelPreMultiplier = affectedByChannelPreMultiplier;
        this.affectedByGlobalPreMultiplier = affectedByGlobalPreMultiplier;
        this.affectedByPostMultiplier = affectedByPostMultiplier;
        this.affectedByMitigation = affectedByMitigation;
    }

    /** Stable lowercase serialized name; ordinal values are never persistent IDs. */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean affectedByMeleeCooldown() {
        return affectedByMeleeCooldown;
    }

    public boolean affectedByMeleeCrit() {
        return affectedByMeleeCrit;
    }

    public boolean affectedByProjectileCrit() {
        return affectedByProjectileCrit;
    }

    public boolean affectedByApplicationPreMultiplier() {
        return affectedByApplicationPreMultiplier;
    }

    public boolean affectedByChannelPreMultiplier() {
        return affectedByChannelPreMultiplier;
    }

    public boolean affectedByGlobalPreMultiplier() {
        return affectedByGlobalPreMultiplier;
    }

    public boolean affectedByPostMultiplier() {
        return affectedByPostMultiplier;
    }

    public boolean affectedByMitigation() {
        return affectedByMitigation;
    }
}
