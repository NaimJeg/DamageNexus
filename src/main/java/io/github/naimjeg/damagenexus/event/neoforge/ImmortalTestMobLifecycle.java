package io.github.naimjeg.damagenexus.event.neoforge;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.command.test.TestMobTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Ordinary-gameplay-damage protection for marked test-exhibit mobs. */
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
                || !TestMobTags.isImmortal(entity)
                || !canProtectFromDeath(event.getSource())) {
            // A pending marker is meaningful only for the ordinary lethal
            // transaction whose death this handler cancels. Forced death must
            // fail closed even if stale external data left the tag behind.
            entity.removeTag(TestMobTags.PENDING_IMMORTAL_RESTORE);
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

    private static boolean canProtectFromDeath(DamageSource source) {
        return !source.is(Tags.DamageTypes.IS_TECHNICAL)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }
}
