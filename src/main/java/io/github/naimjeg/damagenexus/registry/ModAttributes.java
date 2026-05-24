package io.github.naimjeg.damagenexus.registry;

import io.github.naimjeg.damagenexus.DamageNexus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DamageNexus attribute registry.
 *
 * <p>Phase 8 consumes the critical, seven channel-damage, melee/projectile
 * damage, eight channel-resistance, melee/projectile resistance, and thorns
 * attributes. Channel resistance is selected indirectly by the active
 * {@code DamageChannelRegistry.ChannelDefinition}. There is deliberately no
 * {@code physical_damage_additive}; rules may still write the existing
 * {@code physical_damage} pre-multiplier bucket.</p>
 *
 * <p>{@link #VULNERABLE_DAMAGE_ADDITIVE}, {@link #DODGE_CHANCE}, and
 * {@link #HEALING_RECEIVED} retain their IDs, ranges, defaults, and stored
 * modifiers, but are reserved and currently have no framework consumer.</p>
 */
public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, DamageNexus.MODID);

    public static final DeferredHolder<Attribute, Attribute> CRIT_CHANCE = ATTRIBUTES.register("crit_chance",
            () -> new RangedAttribute("attribute.name.damagenexus.crit_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> CRIT_DAMAGE_ADDITIVE = ATTRIBUTES.register("crit_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.crit_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    /** Reserved: no stable vulnerable-context contract exists yet. */
    public static final DeferredHolder<Attribute, Attribute> VULNERABLE_DAMAGE_ADDITIVE = ATTRIBUTES.register("vulnerable_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.vulnerable_damage_additive", 0.20D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> FIRE_DAMAGE_ADDITIVE = ATTRIBUTES.register("fire_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.fire_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> COLD_DAMAGE_ADDITIVE = ATTRIBUTES.register("cold_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.cold_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> LIGHTNING_DAMAGE_ADDITIVE = ATTRIBUTES.register("lightning_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.lightning_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> MAGIC_DAMAGE_ADDITIVE = ATTRIBUTES.register("magic_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.magic_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> WITHER_DAMAGE_ADDITIVE = ATTRIBUTES.register("wither_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.wither_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> POISON_DAMAGE_ADDITIVE = ATTRIBUTES.register("poison_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.poison_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> MELEE_DAMAGE_ADDITIVE = ATTRIBUTES.register("melee_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.melee_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> PROJECTILE_DAMAGE_ADDITIVE = ATTRIBUTES.register("projectile_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.projectile_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> KINETIC_DAMAGE_ADDITIVE = ATTRIBUTES.register("kinetic_damage_additive",
            () -> new RangedAttribute("attribute.name.damagenexus.kinetic_damage_additive", 0.0D, 0.0D, 100.0D).setSyncable(true));

    /** Reserved: damage avoidance requires its own decision lifecycle. */
    public static final DeferredHolder<Attribute, Attribute> DODGE_CHANCE = ATTRIBUTES.register("dodge_chance",
            () -> new RangedAttribute("attribute.name.damagenexus.dodge_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_PHYSICAL = ATTRIBUTES.register("resistance_physical", () -> new RangedAttribute("attribute.name.damagenexus.resistance_physical", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_FIRE = ATTRIBUTES.register("resistance_fire",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_fire", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_COLD = ATTRIBUTES.register("resistance_cold",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_cold", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_LIGHTNING = ATTRIBUTES.register("resistance_lightning",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_lightning", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_MAGIC = ATTRIBUTES.register("resistance_magic",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_magic", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_WITHER = ATTRIBUTES.register("resistance_wither",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_wither", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_POISON = ATTRIBUTES.register("resistance_poison",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_poison", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_MELEE = ATTRIBUTES.register("resistance_melee",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_melee", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_PROJECTILE = ATTRIBUTES.register("resistance_projectile",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_projectile", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> RESISTANCE_KINETIC = ATTRIBUTES.register("resistance_kinetic",
            () -> new RangedAttribute("attribute.name.damagenexus.resistance_kinetic", 0.0D, -10240.0D, 10240.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> THORNS = ATTRIBUTES.register("thorns",
            () -> new RangedAttribute("attribute.name.damagenexus.thorns", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    /** Reserved: healing is outside the damage-settlement pipeline. */
    public static final DeferredHolder<Attribute, Attribute> HEALING_RECEIVED = ATTRIBUTES.register("healing_received",
            () -> new RangedAttribute("attribute.name.damagenexus.healing_received", 1.0D, 0.0D, 10.0D).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}
