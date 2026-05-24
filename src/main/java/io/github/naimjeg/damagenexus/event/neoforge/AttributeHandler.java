package io.github.naimjeg.damagenexus.event.neoforge;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.registry.ModAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

@EventBusSubscriber(modid = DamageNexus.MODID)
public class AttributeHandler {

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {

        for (EntityType<? extends LivingEntity> type : event.getTypes()) {

            event.add(type, ModAttributes.CRIT_CHANCE);
            event.add(type, ModAttributes.CRIT_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.VULNERABLE_DAMAGE_ADDITIVE);

            event.add(type, ModAttributes.FIRE_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.COLD_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.LIGHTNING_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.MAGIC_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.WITHER_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.POISON_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.MELEE_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.PROJECTILE_DAMAGE_ADDITIVE);
            event.add(type, ModAttributes.KINETIC_DAMAGE_ADDITIVE);

            event.add(type, ModAttributes.DODGE_CHANCE);
            event.add(type, ModAttributes.RESISTANCE_PHYSICAL);
            event.add(type, ModAttributes.RESISTANCE_FIRE);
            event.add(type, ModAttributes.RESISTANCE_COLD);
            event.add(type, ModAttributes.RESISTANCE_LIGHTNING);
            event.add(type, ModAttributes.RESISTANCE_MAGIC);
            event.add(type, ModAttributes.RESISTANCE_WITHER);
            event.add(type, ModAttributes.RESISTANCE_POISON);
            event.add(type, ModAttributes.RESISTANCE_MELEE);
            event.add(type, ModAttributes.RESISTANCE_PROJECTILE);
            event.add(type, ModAttributes.RESISTANCE_KINETIC);

            event.add(type, ModAttributes.THORNS);
            event.add(type, ModAttributes.HEALING_RECEIVED);
        }
    }
}
