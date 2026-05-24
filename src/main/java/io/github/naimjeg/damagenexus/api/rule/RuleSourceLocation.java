package io.github.naimjeg.damagenexus.api.rule;

/**
 * Runtime location from which a rule was collected.
 * <p>
 * This is intentionally separate from DamageRuleRole:
 * - source location says where the rule came from;
 * - role says how the rule runs in the current transaction.
 */
public enum RuleSourceLocation {
    ATTACKER_MAINHAND,
    ATTACKER_OFFHAND,
    ATTACKER_HEAD,
    ATTACKER_CHEST,
    ATTACKER_LEGS,
    ATTACKER_FEET,

    VICTIM_MAINHAND,
    VICTIM_OFFHAND,
    VICTIM_HEAD,
    VICTIM_CHEST,
    VICTIM_LEGS,
    VICTIM_FEET,

    PROJECTILE,
    /** Dynamic slot described by RuleExecutionContext.slotSemantic(). */
    EXTERNAL,
    ATTACKER_ENTITY,
    VICTIM_ENTITY,
    DAMAGE_TYPE,
    DATAPACK,
    JAVA_API,
    VANILLA
}

