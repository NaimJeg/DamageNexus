package io.github.naimjeg.damagenexus.api;

import io.github.naimjeg.damagenexus.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Stable, read-only references to DamageNexus attributes. */
public final class DamageNexusAttributes {
    public static final ResourceKey<Attribute> CRIT_CHANCE = key("crit_chance");
    public static final ResourceKey<Attribute> CRIT_DAMAGE_ADDITIVE = key("crit_damage_additive");
    /** Reserved and currently not consumed. */
    public static final ResourceKey<Attribute> VULNERABLE_DAMAGE_ADDITIVE = key("vulnerable_damage_additive");
    public static final ResourceKey<Attribute> FIRE_DAMAGE_ADDITIVE = key("fire_damage_additive");
    public static final ResourceKey<Attribute> COLD_DAMAGE_ADDITIVE = key("cold_damage_additive");
    public static final ResourceKey<Attribute> LIGHTNING_DAMAGE_ADDITIVE = key("lightning_damage_additive");
    public static final ResourceKey<Attribute> MAGIC_DAMAGE_ADDITIVE = key("magic_damage_additive");
    public static final ResourceKey<Attribute> WITHER_DAMAGE_ADDITIVE = key("wither_damage_additive");
    public static final ResourceKey<Attribute> POISON_DAMAGE_ADDITIVE = key("poison_damage_additive");
    public static final ResourceKey<Attribute> KINETIC_DAMAGE_ADDITIVE = key("kinetic_damage_additive");
    public static final ResourceKey<Attribute> MELEE_DAMAGE_ADDITIVE = key("melee_damage_additive");
    public static final ResourceKey<Attribute> PROJECTILE_DAMAGE_ADDITIVE = key("projectile_damage_additive");
    /** Reserved and currently not consumed. */
    public static final ResourceKey<Attribute> DODGE_CHANCE = key("dodge_chance");
    public static final ResourceKey<Attribute> RESISTANCE_PHYSICAL = key("resistance_physical");
    public static final ResourceKey<Attribute> RESISTANCE_FIRE = key("resistance_fire");
    public static final ResourceKey<Attribute> RESISTANCE_COLD = key("resistance_cold");
    public static final ResourceKey<Attribute> RESISTANCE_LIGHTNING = key("resistance_lightning");
    public static final ResourceKey<Attribute> RESISTANCE_MAGIC = key("resistance_magic");
    public static final ResourceKey<Attribute> RESISTANCE_WITHER = key("resistance_wither");
    public static final ResourceKey<Attribute> RESISTANCE_POISON = key("resistance_poison");
    public static final ResourceKey<Attribute> RESISTANCE_KINETIC = key("resistance_kinetic");
    public static final ResourceKey<Attribute> RESISTANCE_MELEE = key("resistance_melee");
    public static final ResourceKey<Attribute> RESISTANCE_PROJECTILE = key("resistance_projectile");
    public static final ResourceKey<Attribute> THORNS = key("thorns");
    /** Reserved and currently not consumed. */
    public static final ResourceKey<Attribute> HEALING_RECEIVED = key("healing_received");

    private DamageNexusAttributes() {}

    /** Holder accessors are valid after NeoForge has bound the attribute registry. */
    public static Holder<Attribute> critChance() { return ModAttributes.CRIT_CHANCE; }
    public static Holder<Attribute> critDamageAdditive() { return ModAttributes.CRIT_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> vulnerableDamageAdditive() { return ModAttributes.VULNERABLE_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> fireDamageAdditive() { return ModAttributes.FIRE_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> coldDamageAdditive() { return ModAttributes.COLD_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> lightningDamageAdditive() { return ModAttributes.LIGHTNING_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> magicDamageAdditive() { return ModAttributes.MAGIC_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> witherDamageAdditive() { return ModAttributes.WITHER_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> poisonDamageAdditive() { return ModAttributes.POISON_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> kineticDamageAdditive() { return ModAttributes.KINETIC_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> meleeDamageAdditive() { return ModAttributes.MELEE_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> projectileDamageAdditive() { return ModAttributes.PROJECTILE_DAMAGE_ADDITIVE; }
    public static Holder<Attribute> dodgeChance() { return ModAttributes.DODGE_CHANCE; }
    public static Holder<Attribute> resistancePhysical() { return ModAttributes.RESISTANCE_PHYSICAL; }
    public static Holder<Attribute> resistanceFire() { return ModAttributes.RESISTANCE_FIRE; }
    public static Holder<Attribute> resistanceCold() { return ModAttributes.RESISTANCE_COLD; }
    public static Holder<Attribute> resistanceLightning() { return ModAttributes.RESISTANCE_LIGHTNING; }
    public static Holder<Attribute> resistanceMagic() { return ModAttributes.RESISTANCE_MAGIC; }
    public static Holder<Attribute> resistanceWither() { return ModAttributes.RESISTANCE_WITHER; }
    public static Holder<Attribute> resistancePoison() { return ModAttributes.RESISTANCE_POISON; }
    public static Holder<Attribute> resistanceKinetic() { return ModAttributes.RESISTANCE_KINETIC; }
    public static Holder<Attribute> resistanceMelee() { return ModAttributes.RESISTANCE_MELEE; }
    public static Holder<Attribute> resistanceProjectile() { return ModAttributes.RESISTANCE_PROJECTILE; }
    public static Holder<Attribute> thorns() { return ModAttributes.THORNS; }
    public static Holder<Attribute> healingReceived() { return ModAttributes.HEALING_RECEIVED; }

    private static ResourceKey<Attribute> key(String path) {
        Identifier id = DamageNexusIds.id(path);
        return ResourceKey.create(Registries.ATTRIBUTE, id);
    }
}
