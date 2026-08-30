package io.github.naimjeg.damagenexus.command.test;

import io.github.naimjeg.damagenexus.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Test-command resistance type mapping. Exclusively a test facility: each
 * value maps by name to one stable DamageNexus resistance attribute, without
 * string/reflection field access.
 */
public enum TestResistance {
    PHYSICAL("physical", ModAttributes.RESISTANCE_PHYSICAL),
    FIRE("fire", ModAttributes.RESISTANCE_FIRE),
    COLD("cold", ModAttributes.RESISTANCE_COLD),
    LIGHTNING("lightning", ModAttributes.RESISTANCE_LIGHTNING),
    MAGIC("magic", ModAttributes.RESISTANCE_MAGIC),
    WITHER("wither", ModAttributes.RESISTANCE_WITHER),
    POISON("poison", ModAttributes.RESISTANCE_POISON),
    MELEE("melee", ModAttributes.RESISTANCE_MELEE),
    PROJECTILE("projectile", ModAttributes.RESISTANCE_PROJECTILE),
    KINETIC("kinetic", ModAttributes.RESISTANCE_KINETIC);

    /** Shared legal range for every DamageNexus resistance attribute. */
    public static final double MIN_ATTRIBUTE_VALUE = -10240.0D;
    public static final double MAX_ATTRIBUTE_VALUE = 10240.0D;

    private final String commandName;
    private final Holder<Attribute> attribute;

    TestResistance(String commandName, Holder<Attribute> attribute) {
        this.commandName = commandName;
        this.attribute = attribute;
    }

    public String commandName() {
        return commandName;
    }

    public Holder<Attribute> attribute() {
        return attribute;
    }
}
