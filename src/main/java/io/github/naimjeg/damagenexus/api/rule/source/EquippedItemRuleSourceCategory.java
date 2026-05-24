package io.github.naimjeg.damagenexus.api.rule.source;

/**
 * Generic slot-matching category for an external item contribution.
 * PROJECTILE represents the captured projectile weapon/source family; when a
 * vanilla captured weapon already exists, an external PROJECTILE contribution
 * is suppressed to avoid executing the same physical weapon twice. Generic
 * projectile-compatible equipment should use ITEM or WEAPON as appropriate.
 */
public enum EquippedItemRuleSourceCategory {
    ITEM,
    WEAPON,
    ARMOR,
    PROJECTILE
}
