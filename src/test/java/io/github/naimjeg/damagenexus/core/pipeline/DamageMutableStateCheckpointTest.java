package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.DamageComponent;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.api.critical.*;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageMutableStateCheckpointTest {

    private static final float EPSILON = 0.0001f;

    @Test
    void packetCheckpointRestoresComponentsBucketsAndTemporaryResistance() {
        DamagePacketState packet = new DamagePacketState();
        DamageComponent component = packet.getOrCreateComponent(
                DamageChannelRegistry.getUntyped()
        );
        component.addBase(DamageApplicationBucket.VANILLA_OTHER_BASE, 2.0f);
        component.addPostMultiplier(0.25f);
        component.addMitigation(0.10f);
        component.addTemporaryResistance(7.0f);
        packet.addGlobalPostMultiplier(0.50f);
        packet.addGlobalMitigation(0.20f);
        DamagePacketState.Checkpoint checkpoint = packet.checkpoint();

        component.addBase(DamageApplicationBucket.DN_RULE_BASE, 100.0f);
        component.addPostMultiplier(4.0f);
        component.addMitigation(0.90f);
        component.addTemporaryResistance(13.0f);
        packet.addGlobalPostMultiplier(5.0f);
        packet.addGlobalMitigation(0.70f);

        packet.restore(checkpoint);

        assertEquals(1, packet.activeComponentCount());
        assertEquals(2.0f, component.getBaseAmount(), EPSILON);
        assertEquals(
                7.0f,
                component.getTemporaryResistanceRating(),
                EPSILON
        );
        assertEquals(1, packet.globalPostMultipliers().size());
        assertEquals(
                0.50f,
                packet.globalPostMultipliers().getFloat(0),
                EPSILON
        );
        assertEquals(1, packet.globalMitigations().size());
        assertEquals(
                0.20f,
                packet.globalMitigations().getFloat(0),
                EPSILON
        );
    }

    @Test
    void combatResultAndPhaseCheckpointsRestoreEveryFlagAndValue() {
        DamageCombatState combat = new DamageCombatState();
        DamagePipelineResult result = new DamagePipelineResult();
        DamagePhaseState phase = new DamagePhaseState();
        DamageCombatState.Checkpoint combatCheckpoint = combat.checkpoint();
        DamagePipelineResult.Checkpoint resultCheckpoint = result.checkpoint();
        DamagePhaseState.Checkpoint phaseCheckpoint = phase.checkpoint();

        combat.criticalDecision().add(new CriticalDecisionContribution(
                Identifier.fromNamespaceAndPath("test", "force"),
                10,
                CriticalDecision.FORCE_CRITICAL
        ));
        combat.criticalDecision().freeze(
                CriticalDecision.FORCE_CRITICAL,
                true,
                CriticalDecisionOutcome.FORCED,
                false
        );
        combat.criticalDecision().markEffectApplied();
        assertTrue(combat.claimAttributeScaling());
        assertFalse(combat.claimAttributeScaling());
        combat.markArmorHandled();
        combat.multiplyArmorEffectiveness(0.25f);
        result.setOffensiveTotal(12.0f);
        result.setFinalEventDamage(8.0f);
        result.cancel("test/cancel");
        phase.setCurrentPhase(DamagePhase.FINAL_OVERRIDE);
        phase.lockOffense();
        phase.markDefenseCalculatedAndLocked();

        combat.restore(combatCheckpoint);
        result.restore(resultCheckpoint);
        phase.restore(phaseCheckpoint);

        assertFalse(combat.critical());
        assertFalse(combat.criticalDecision().snapshot().frozen());
        assertEquals(0, combat.criticalDecision().snapshot().contributions().size());
        assertFalse(combat.attributeScalingApplied());
        assertTrue(combat.claimAttributeScaling());
        assertFalse(combat.armorHandled());
        assertEquals(1.0f, combat.armorEffectivenessMultiplier(), EPSILON);
        assertEquals(0.0f, result.offensiveTotal(), EPSILON);
        assertEquals(0.0f, result.finalEventDamage(), EPSILON);
        assertFalse(result.damageCancelled());
        assertEquals(null, result.cancelSourceId());
        assertEquals(DamagePhase.BASE_MODIFICATION, phase.currentPhase());
        assertFalse(phase.offensiveLocked());
        assertFalse(phase.defensiveLocked());
        assertFalse(phase.defenseCalculated());
    }
}
