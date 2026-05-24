package io.github.naimjeg.damagenexus.bridge.vanilla;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaBridgePlanTest {
    @Test
    void zeroBonusCriticalProjectileStillEntersCriticalRebuildPath() {
        VanillaDamageCapture.OffensiveSnapshot snapshot = snapshot(true);

        assertTrue(VanillaBridgePlan.from(
                2.0f, null, snapshot, 0.0f, 0.0f).rebuildPreEventDelta());
    }

    @Test
    void zeroBonusNonCriticalProjectileDoesNotInventARebuild() {
        VanillaDamageCapture.OffensiveSnapshot snapshot = snapshot(false);

        assertFalse(VanillaBridgePlan.from(
                2.0f, null, snapshot, 0.0f, 0.0f).rebuildPreEventDelta());
    }

    private static VanillaDamageCapture.OffensiveSnapshot snapshot(
            boolean critical
    ) {
        return new VanillaDamageCapture.OffensiveSnapshot(
                null,
                null,
                null,
                ItemStack.EMPTY,
                2.0f,
                2.0f,
                0.0f,
                2.0f,
                VanillaDamageCapture.PreEventDelta.none(2.0f, 2.0f),
                0.0f,
                0.0f,
                critical
        );
    }
}
