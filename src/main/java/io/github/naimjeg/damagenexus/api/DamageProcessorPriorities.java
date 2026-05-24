package io.github.naimjeg.damagenexus.api;

/**
 * Central priority table for built-in DamageNexus phase processors.
 * <p>
 * Higher values run earlier within the same DamagePhase.
 */
public final class DamageProcessorPriorities {

    public static final int VANILLA_INITIAL_BASE = 1200;
    /**
     * Adds DamageNexus thorns attribute as extra base damage to vanilla Thorns
     * reflection.
     */
    public static final int VANILLA_THORNS_ATTRIBUTE_BRIDGE = 1190;
    public static final int VANILLA_ARMOR_EFFECTIVENESS = 1010;
    public static final int VANILLA_DAMAGE_PROTECTION = 1005;
    public static final int VANILLA_RESISTANCE_EFFECT = 1004;
    public static final int RULE_MITIGATION = 1002;
    public static final int DN_ARMOR_MITIGATION = 1000;
    public static final int DN_RESISTANCE_MITIGATION = 999;
    public static final int VANILLA_DIFFICULTY_SCALING = 1000;
    public static final int VANILLA_OFFENSIVE_MOB_EFFECT = 990;
    public static final int VANILLA_WEAPON_SPECIAL_BONUS = 985;
    public static final int VANILLA_OFFENSIVE_ENCHANTMENT = 980;
    public static final int VANILLA_PLAYER_ATTACK_SCALING = 970;
    public static final int VANILLA_PROJECTILE_SCALING = 960;
    public static final int RULES = 500;

    private DamageProcessorPriorities() {
    }
}

