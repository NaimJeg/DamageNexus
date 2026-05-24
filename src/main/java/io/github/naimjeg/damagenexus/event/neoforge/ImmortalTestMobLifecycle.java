package io.github.naimjeg.damagenexus.event.neoforge;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.command.test.TestMobTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Death-only protection for explicitly marked test-exhibit mobs. */
@EventBusSubscriber(modid = DamageNexus.MODID)
public final class ImmortalTestMobLifecycle {

    private ImmortalTestMobLifecycle() {
    }

    /**
     * NeoForge 26.1 posts this after health and all damage reductions have
     * been applied, but before LivingEntity marks itself dead or creates
     * drops/experience. Health restoration is deferred until Post so the
     * settlement and diagnostic observers retain the lethal health delta.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventExhibitDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel)
                || !TestMobTags.isImmortal(entity)) {
            return;
        }

        entity.addTag(TestMobTags.PENDING_IMMORTAL_RESTORE);
        event.setCanceled(true);
    }

    /** Restore only after every normal DamageNexus Post observer has run. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreAfterDamagePost(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!entity.removeTag(TestMobTags.PENDING_IMMORTAL_RESTORE)) {
            return;
        }
        if (!TestMobTags.isImmortal(entity) || entity.isRemoved()) {
            return;
        }

        float maximum = entity.getMaxHealth();
        if (!Float.isFinite(maximum) || maximum <= 0.0F) {
            maximum = 1.0F;
        }
        entity.setHealth(maximum);
        if (entity.getPose() == Pose.DYING) {
            entity.setPose(Pose.STANDING);
        }
    }
}
