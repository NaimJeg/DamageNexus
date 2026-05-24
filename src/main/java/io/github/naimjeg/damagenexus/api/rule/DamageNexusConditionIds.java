package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import net.minecraft.resources.Identifier;

/** Stable serialized identifiers for built-in DamageNexus rule conditions. */
public final class DamageNexusConditionIds {
    public static final Identifier DAMAGE_TYPE_TAG = id("damage_type_tag");
    public static final Identifier DAMAGE_TYPE_IS = id("damage_type_is");
    public static final Identifier ATTACKER_HAS_EFFECT = id("attacker_has_effect");
    public static final Identifier TARGET_HAS_EFFECT = id("target_has_effect");
    public static final Identifier ATTACKER_EFFECT_TAG = id("attacker_effect_tag");
    public static final Identifier TARGET_EFFECT_TAG = id("target_effect_tag");
    public static final Identifier SOURCE_ACTION_IS = id("source_action_is");
    public static final Identifier SOURCE_TAG = id("source_tag");
    public static final Identifier REQUEST_KIND_IS = id("request_kind_is");
    public static final Identifier IS_PRIMARY_DAMAGE = id("is_primary_damage");
    public static final Identifier IS_PROC_DAMAGE = id("is_proc_damage");
    public static final Identifier HAS_PARENT_DAMAGE = id("has_parent_damage");
    public static final Identifier PROC_ALLOWED = id("proc_allowed");
    public static final Identifier DAMAGE_CHANNEL_IS = id("damage_channel_is");
    public static final Identifier ALWAYS = id("always");
    public static final Identifier ALL_OF = id("all_of");
    public static final Identifier ANY_OF = id("any_of");
    public static final Identifier NOT = id("not");
    public static final Identifier IS_CRITICAL = id("is_critical");
    public static final Identifier TARGET_ON_FIRE = id("target_on_fire");
    public static final Identifier ATTACKER_HEALTH_BELOW = id("attacker_health_below");
    public static final Identifier TARGET_HEALTH_BELOW = id("target_health_below");
    public static final Identifier ATTACKER_HEALTH_ABOVE = id("attacker_health_above");
    public static final Identifier TARGET_HEALTH_ABOVE = id("target_health_above");
    public static final Identifier TARGET_ENTITY_TYPE_IS = id("target_entity_type_is");
    public static final Identifier ATTACKER_ENTITY_TYPE_IS = id("attacker_entity_type_is");
    public static final Identifier TARGET_ENTITY_TYPE_TAG = id("target_entity_type_tag");
    public static final Identifier ATTACKER_ENTITY_TYPE_TAG = id("attacker_entity_type_tag");
    public static final Identifier TARGET_MOB_CATEGORY_IS = id("target_mob_category_is");
    public static final Identifier ATTACKER_MOB_CATEGORY_IS = id("attacker_mob_category_is");
    public static final Identifier TARGET_IS_BOSS = id("target_is_boss");
    public static final Identifier ATTACKER_IS_BOSS = id("attacker_is_boss");

    private DamageNexusConditionIds() {}
    private static Identifier id(String path) { return DamageNexusIds.id(path); }
}
