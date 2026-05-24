package io.github.naimjeg.damagenexus.command.test;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;

public final class TestMobFactory {

    private static final int TEN_MINUTES = 20 * 60 * 10;

    private TestMobFactory() {
    }

    public static Cow cow(
            ServerLevel level,
            Vec3 pos,
            String name
    ) {
        Cow cow = EntityType.COW.create(level, EntitySpawnReason.COMMAND);

        if (cow == null) {
            return null;
        }

        setupMob(cow, pos, name);
        level.addFreshEntity(cow);

        return cow;
    }

    public static Spider spider(
            ServerLevel level,
            Vec3 pos,
            String name
    ) {
        Spider spider = EntityType.SPIDER.create(level, EntitySpawnReason.COMMAND);

        if (spider == null) {
            return null;
        }

        setupMob(spider, pos, name);
        level.addFreshEntity(spider);

        return spider;
    }

    public static Zombie zombie(
            ServerLevel level,
            Vec3 pos,
            String name,
            ArmorSet armorSet,
            boolean protectionIv,
            boolean resistance
    ) {
        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);

        if (zombie == null) {
            return null;
        }

        setupMob(zombie, pos, name);

        if (armorSet != null && armorSet != ArmorSet.NONE) {
            equipArmor(level, zombie, armorSet, protectionIv);
        }

        if (resistance) {
            zombie.addEffect(new MobEffectInstance(
                    MobEffects.RESISTANCE,
                    TEN_MINUTES,
                    0,
                    false,
                    true
            ));
        }

        level.addFreshEntity(zombie);

        return zombie;
    }

    public static Zombie freeZombie(
            ServerLevel level,
            Vec3 pos,
            String name
    ) {
        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);

        if (zombie == null) {
            return null;
        }

        setupMob(zombie, pos, name);
        zombie.setNoAi(false);

        level.addFreshEntity(zombie);

        return zombie;
    }

    public static void sanitizeLiving(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        entity.removeAllEffects();
        entity.setRemainingFireTicks(0);
        entity.clearFire();

        entity.setAbsorptionAmount(0.0f);
        entity.invulnerableTime = 0;
        entity.hurtTime = 0;
        entity.hurtDuration = 0;

        entity.setHealth(entity.getMaxHealth());
    }

    public static void sanitizePlayer(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        entity.removeEffect(MobEffects.STRENGTH);
        entity.removeEffect(MobEffects.WEAKNESS);
        entity.setRemainingFireTicks(0);
        entity.clearFire();
        entity.invulnerableTime = 0;
    }

    private static void setupMob(
            Mob mob,
            Vec3 pos,
            String name
    ) {
        mob.setPos(pos.x, pos.y, pos.z);
        mob.setYRot(0.0F);
        mob.setCustomName(Component.literal(name));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        mob.setNoAi(true);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }

        sanitizeLiving(mob);
    }

    private static void equipArmor(
            ServerLevel level,
            LivingEntity entity,
            ArmorSet armorSet,
            boolean protectionIv
    ) {
        Holder<Enchantment> protection = null;

        if (protectionIv) {
            protection = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.PROTECTION)
                    .orElse(null);
        }

        entity.setItemSlot(
                EquipmentSlot.HEAD,
                createArmorStack(armorSet.helmet, protection)
        );

        entity.setItemSlot(
                EquipmentSlot.CHEST,
                createArmorStack(armorSet.chest, protection)
        );

        entity.setItemSlot(
                EquipmentSlot.LEGS,
                createArmorStack(armorSet.legs, protection)
        );

        entity.setItemSlot(
                EquipmentSlot.FEET,
                createArmorStack(armorSet.boots, protection)
        );
    }

    private static ItemStack createArmorStack(
            Item item,
            Holder<Enchantment> protection
    ) {
        ItemStack stack = new ItemStack(item);

        if (protection != null) {
            ItemEnchantments.Mutable mutableEnchantments =
                    new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

            mutableEnchantments.set(protection, 4);

            stack.set(
                    DataComponents.ENCHANTMENTS,
                    mutableEnchantments.toImmutable()
            );
        }

        return stack;
    }

    public enum ArmorSet {
        NONE(null, null, null, null),

        IRON(
                Items.IRON_HELMET,
                Items.IRON_CHESTPLATE,
                Items.IRON_LEGGINGS,
                Items.IRON_BOOTS
        ),

        DIAMOND(
                Items.DIAMOND_HELMET,
                Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS,
                Items.DIAMOND_BOOTS
        ),

        NETHERITE(
                Items.NETHERITE_HELMET,
                Items.NETHERITE_CHESTPLATE,
                Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_BOOTS
        );

        final Item helmet;
        final Item chest;
        final Item legs;
        final Item boots;

        ArmorSet(
                Item helmet,
                Item chest,
                Item legs,
                Item boots
        ) {
            this.helmet = helmet;
            this.chest = chest;
            this.legs = legs;
            this.boots = boots;
        }
    }
}
