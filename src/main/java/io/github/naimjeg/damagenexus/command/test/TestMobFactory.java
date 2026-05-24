package io.github.naimjeg.damagenexus.command.test;

import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.turtle.Turtle;
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

    public static SpawnResult spawnPreset(
            ServerLevel level,
            TestMobPreset preset,
            Vec3 pos,
            TestMobSpawnOptions options
    ) {
        SpawnFailure validation = validateSpawnPosition(level, pos);
        if (validation != null) {
            return SpawnResult.failure(preset, pos, options, validation);
        }

        Mob mob = createPresetMob(level, preset, pos, options);
        if (mob == null) {
            return SpawnResult.failure(
                    preset,
                    pos,
                    options,
                    SpawnFailure.ENTITY_CREATION_FAILED
            );
        }

        if (!level.addFreshEntity(mob)) {
            mob.discard();
            return SpawnResult.failure(
                    preset,
                    pos,
                    options,
                    SpawnFailure.ADD_TO_LEVEL_FAILED
            );
        }

        return SpawnResult.success(preset, pos, options, mob);
    }

    public static SpawnFailure validateSpawnPosition(
            ServerLevel level,
            Vec3 pos
    ) {
        if (level == null || pos == null
                || !Double.isFinite(pos.x)
                || !Double.isFinite(pos.y)
                || !Double.isFinite(pos.z)) {
            return SpawnFailure.NON_FINITE_POSITION;
        }
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return SpawnFailure.OUTSIDE_WORLD_BORDER;
        }

        var blockPos = net.minecraft.core.BlockPos.containing(pos);
        if (!level.isInWorldBounds(blockPos)) {
            return SpawnFailure.OUTSIDE_BUILD_HEIGHT;
        }
        if (!level.getChunkSource().hasChunk(
                SectionPos.blockToSectionCoord(blockPos.getX()),
                SectionPos.blockToSectionCoord(blockPos.getZ())
        )) {
            return SpawnFailure.CHUNK_NOT_LOADED;
        }
        return null;
    }

    private static Mob createPresetMob(
            ServerLevel level,
            TestMobPreset preset,
            Vec3 pos,
            TestMobSpawnOptions options
    ) {
        Mob mob = switch (preset) {
            case BASELINE, COW -> EntityType.COW.create(
                    level,
                    EntitySpawnReason.COMMAND
            );
            case SPIDER -> EntityType.SPIDER.create(
                    level,
                    EntitySpawnReason.COMMAND
            );
            default -> EntityType.ZOMBIE.create(
                    level,
                    EntitySpawnReason.COMMAND
            );
        };
        if (mob == null) {
            return null;
        }

        setupMob(mob, pos, Component.literal(preset.displayName()), options);
        if (mob instanceof Zombie zombie) {
            switch (preset) {
                case IRON -> equipArmor(level, zombie, ArmorSet.IRON, false);
                case DIAMOND -> equipArmor(
                        level,
                        zombie,
                        ArmorSet.DIAMOND,
                        false
                );
                case NETHERITE_PROT -> equipArmor(
                        level,
                        zombie,
                        ArmorSet.NETHERITE,
                        true
                );
                case LOW_HP -> zombie.setHealth(5.0F);
                case INVUL -> zombie.invulnerableTime = 10;
                default -> {
                }
            }
        }
        return mob;
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
        return addOrDiscard(level, cow);
    }

    public static Turtle turtle(
            ServerLevel level,
            Vec3 pos,
            Component name
    ) {
        Turtle turtle = EntityType.TURTLE.create(
                level,
                EntitySpawnReason.COMMAND
        );

        if (turtle == null) {
            return null;
        }

        setupMob(turtle, pos, name);
        return addOrDiscard(level, turtle);
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
        return addOrDiscard(level, spider);
    }

    public static Zombie zombie(
            ServerLevel level,
            Vec3 pos,
            String name,
            ArmorSet armorSet,
            boolean protectionIv,
            boolean resistance
    ) {
        return zombie(
                level,
                pos,
                name,
                armorSet,
                protectionIv,
                resistance,
                TestMobSpawnOptions.DEFAULT
        );
    }

    public static Zombie zombie(
            ServerLevel level,
            Vec3 pos,
            String name,
            ArmorSet armorSet,
            boolean protectionIv,
            boolean resistance,
            TestMobSpawnOptions options
    ) {
        return zombie(
                level,
                pos,
                Component.literal(name),
                armorSet,
                protectionIv,
                resistance,
                options
        );
    }

    public static Zombie zombie(
            ServerLevel level,
            Vec3 pos,
            Component name,
            ArmorSet armorSet,
            boolean protectionIv,
            boolean resistance
    ) {
        return zombie(
                level,
                pos,
                name,
                armorSet,
                protectionIv,
                resistance,
                TestMobSpawnOptions.DEFAULT
        );
    }

    public static Zombie zombie(
            ServerLevel level,
            Vec3 pos,
            Component name,
            ArmorSet armorSet,
            boolean protectionIv,
            boolean resistance,
            TestMobSpawnOptions options
    ) {
        Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);

        if (zombie == null) {
            return null;
        }

        setupMob(zombie, pos, name, options);

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

        return addOrDiscard(level, zombie);
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
        return addOrDiscard(level, zombie);
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
        setupMob(
                mob,
                pos,
                Component.literal(name),
                TestMobSpawnOptions.DEFAULT
        );
    }

    private static void setupMob(
            Mob mob,
            Vec3 pos,
            Component name
    ) {
        setupMob(mob, pos, name, TestMobSpawnOptions.DEFAULT);
    }

    private static void setupMob(
            Mob mob,
            Vec3 pos,
            Component name,
            TestMobSpawnOptions options
    ) {
        mob.setPos(pos.x, pos.y, pos.z);
        mob.setYRot(0.0F);
        mob.setCustomName(name);
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        mob.setNoAi(true);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }

        sanitizeLiving(mob);
        options.apply(mob);
    }

    private static <T extends Mob> T addOrDiscard(
            ServerLevel level,
            T mob
    ) {
        if (level.addFreshEntity(mob)) {
            return mob;
        }
        mob.discard();
        return null;
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

    public enum SpawnFailure {
        NON_FINITE_POSITION("non_finite_position"),
        OUTSIDE_BUILD_HEIGHT("outside_build_height"),
        OUTSIDE_WORLD_BORDER("outside_world_border"),
        CHUNK_NOT_LOADED("chunk_not_loaded"),
        ENTITY_CREATION_FAILED("entity_creation_failed"),
        ADD_TO_LEVEL_FAILED("add_to_level_failed");

        private final String translationSuffix;

        SpawnFailure(String translationSuffix) {
            this.translationSuffix = translationSuffix;
        }

        public String translationKey() {
            return "command."
                    + "damagenexus.spawn_failure."
                    + translationSuffix;
        }
    }

    public record SpawnResult(
            TestMobPreset preset,
            Vec3 position,
            TestMobSpawnOptions options,
            Mob entity,
            SpawnFailure failure
    ) {
        static SpawnResult success(
                TestMobPreset preset,
                Vec3 position,
                TestMobSpawnOptions options,
                Mob entity
        ) {
            return new SpawnResult(preset, position, options, entity, null);
        }

        static SpawnResult failure(
                TestMobPreset preset,
                Vec3 position,
                TestMobSpawnOptions options,
                SpawnFailure failure
        ) {
            return new SpawnResult(preset, position, options, null, failure);
        }

        public boolean succeeded() {
            return entity != null && failure == null;
        }
    }
}
