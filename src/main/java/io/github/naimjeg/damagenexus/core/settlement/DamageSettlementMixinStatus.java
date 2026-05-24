package io.github.naimjeg.damagenexus.core.settlement;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

/** Low-cost runtime proof that the settlement mixin transformed LivingEntity. */
@ApiStatus.Internal
public final class DamageSettlementMixinStatus {

    private DamageSettlementMixinStatus() {
    }

    public static boolean isApplied() {
        return Marker.class.isAssignableFrom(LivingEntity.class);
    }

    /** Marker added to LivingEntity by the required settlement mixin. */
    public interface Marker {
    }
}
