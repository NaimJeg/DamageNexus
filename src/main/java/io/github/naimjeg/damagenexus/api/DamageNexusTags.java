package io.github.naimjeg.damagenexus.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public class DamageNexusTags {

    public static final TagKey<Item> SPEARS = createItem("spears");

    private static TagKey<Item> createItem(String name) {
        return TagKey.create(
                Registries.ITEM,
                DamageNexusIds.id(name)
        );
    }

    private static TagKey<EntityType<?>> createEntityType(String name) {
        return TagKey.create(
                Registries.ENTITY_TYPE,
                DamageNexusIds.id(name)
        );
    }

    private static TagKey<DamageType> createDamageType(String name) {
        return TagKey.create(
                Registries.DAMAGE_TYPE,
                DamageNexusIds.id(name)
        );
    }

    public static final class EntityTypes {
        public static final TagKey<EntityType<?>> BOSSES =
                createEntityType("bosses");

        private EntityTypes() {
        }
    }

    public static class DamageTypes {
        public static final TagKey<DamageType> BYPASSES_DAMAGENEXUS =
                createDamageType("bypasses_damagenexus");

        public static final TagKey<DamageType> IS_PHYSICAL = createDamageType("is_physical");
        public static final TagKey<DamageType> IS_PROJECTILE = createDamageType("is_projectile");
        public static final TagKey<DamageType> IS_RANGED = createDamageType("is_ranged");
        public static final TagKey<DamageType> IS_SPELL = createDamageType("is_spell");
        public static final TagKey<DamageType> IS_EXPLOSION = createDamageType("is_explosion");
        public static final TagKey<DamageType> IS_ENVIRONMENTAL = createDamageType("is_environmental");
        public static final TagKey<DamageType> IS_MAGIC = createDamageType("is_magic");
        public static final TagKey<DamageType> IS_COLD = createDamageType("is_cold");
        public static final TagKey<DamageType> IS_FIRE = createDamageType("is_fire");
        public static final TagKey<DamageType> IS_LIGHTNING = createDamageType("is_lightning");
        public static final TagKey<DamageType> IS_POISON = createDamageType("is_poison");
        public static final TagKey<DamageType> IS_WITHER = createDamageType("is_wither");
        public static final TagKey<DamageType> IS_KINETIC = createDamageType("is_kinetic");
        public static final TagKey<DamageType> IS_MELEE = createDamageType("is_melee");
        public static final TagKey<DamageType> IS_MACE_SMASH =
                createDamageType("is_mace_smash");

        public static final TagKey<DamageType> IS_SPEAR_STAB =
                createDamageType("is_spear_stab");

        public static final TagKey<DamageType> IS_SPEAR_CHARGE =
                createDamageType("is_spear_charge");

        public static final TagKey<DamageType> IS_SPEAR_ATTACK =
                createDamageType("is_spear_attack");

        private DamageTypes() {
        }
    }
}
