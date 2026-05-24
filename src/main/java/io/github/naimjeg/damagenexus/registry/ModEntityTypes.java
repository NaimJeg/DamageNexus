package io.github.naimjeg.damagenexus.registry;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.entity.DamageDummyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DamageNexus entity type registry.
 *
 * <p>Only the damage dummy is registered here; it is a development/testing
 * LivingEntity target, not a content feature.</p>
 */
public final class ModEntityTypes {

    public static final DeferredRegister.Entities ENTITIES =
            DeferredRegister.createEntities(DamageNexus.MODID);

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<DamageDummyEntity>
            > DAMAGE_DUMMY =
            ENTITIES.registerEntityType(
                    "damage_dummy",
                    DamageDummyEntity::new,
                    MobCategory.MISC,
                    builder -> builder
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .noLootTable()
            );

    private ModEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
