package io.github.naimjeg.damagenexus.api.rule;

public enum DamageRuleProviderType {
    /**
     * Rules supplied by item stacks in an equipment/source slot.
     * <p>
     * The exact slot is described by RuleSourceLocation, not by this provider
     * type.
     */
    ITEM_EQUIPMENT,

    /**
     * Rules supplied by the projectile source reconstructed from the damage
     * source / captured offensive snapshot.
     */
    PROJECTILE_SOURCE,

    /** Item stack supplied by a registered external equipment source. */
    EXTERNAL_ITEM_SOURCE,

    /**
     * Rules supplied by an entity-level source.
     * <p>
     * Reserved for future entity attachments/components.
     */
    ENTITY,

    VANILLA_ENCHANTMENT,
    VANILLA_MOB_EFFECT,
    CUSTOM_MOD_EFFECT,

    /**
     * Reserved source identity for a future damage-type rule provider.
     *
     * <p>DamageNexus currently has no damage-type entry/affix storage; global
     * datapack and Java API rules use their dedicated provider types.</p>
     */
    DAMAGE_TYPE,
    DATAPACK_RULE,

    /**
     * Programmatic rules registered through the public Java API.
     */
    JAVA_API
}

