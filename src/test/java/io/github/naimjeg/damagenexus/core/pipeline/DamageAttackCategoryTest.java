package io.github.naimjeg.damagenexus.core.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageAttackCategoryTest {
    @Test
    void projectileSignalsAlwaysWinOverMeleeSignals() {
        assertEquals(DamageAttackCategory.PROJECTILE,
                classify(true, false, false, false, false, false, false, false));
        assertEquals(DamageAttackCategory.PROJECTILE,
                classify(false, true, false, false, false, false, true, true));
        assertEquals(DamageAttackCategory.PROJECTILE,
                classify(false, false, true, false, false, false, false, false));
        assertEquals(DamageAttackCategory.PROJECTILE,
                classify(false, false, false, true, false, false, false, false));
        assertEquals(DamageAttackCategory.PROJECTILE,
                classify(false, false, false, false, false, true, true, true));
        assertEquals(DamageAttackCategory.PROJECTILE,
                classify(false, false, false, false, true, false, true, true));
    }

    @Test
    void directPlayerAndMobAttackSignalsAreMelee() {
        assertEquals(DamageAttackCategory.MELEE,
                classify(false, false, false, false, false, false, true, false));
        assertEquals(DamageAttackCategory.MELEE,
                classify(false, false, false, false, false, false, false, true));
    }

    @Test
    void environmentalAndUnsignaledDamageHasNoCategory() {
        assertEquals(DamageAttackCategory.NONE,
                classify(false, false, false, false, false, false, false, false));
    }

    private static DamageAttackCategory classify(
            boolean vanillaProjectileTag,
            boolean nexusProjectileTag,
            boolean rangedTag,
            boolean directProjectile,
            boolean authoritativeDirectProjectile,
            boolean vanillaProfileProjectile,
            boolean meleeTag,
            boolean directPlayerOrMobAttack
    ) {
        return DamageAttackCategory.fromSignals(
                vanillaProjectileTag,
                nexusProjectileTag,
                rangedTag,
                directProjectile,
                authoritativeDirectProjectile,
                vanillaProfileProjectile,
                meleeTag,
                directPlayerOrMobAttack
        );
    }
}
