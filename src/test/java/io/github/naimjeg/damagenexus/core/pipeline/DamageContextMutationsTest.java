package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.diagnostics.logging.CombatTraceFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageContextMutationsTest {

    @Test
    void explicitNullBaseBucketIsRejected() {
        DamagePacketState packet = new DamagePacketState();
        DamageContextMutations mutations =
                mutations(packet, new DamagePipelineResult(), DamagePhase.BASE_MODIFICATION);

        DamageMutationResult result = mutations.tryAddBaseDamage(
                DamageChannelRegistry.getUntyped(),
                null,
                2.0f,
                "test/source"
        );

        assertEquals(DamageMutationResult.REJECTED_NULL_APPLICATION_BUCKET, result);
        assertEquals(0, packet.activeComponentCount());
    }

    @Test
    void defaultBaseDamageOverloadUsesRuleBaseBucket() {
        DamagePacketState packet = new DamagePacketState();
        DamageContextMutations mutations =
                mutations(packet, new DamagePipelineResult(), DamagePhase.BASE_MODIFICATION);
        DamageChannel channel = DamageChannelRegistry.getUntyped();

        DamageMutationResult result =
                mutations.tryAddBaseDamage(channel, 2.0f, "test/source");

        assertEquals(DamageMutationResult.APPLIED, result);
        assertEquals(
                2.0f,
                packet.activeComponent(0)
                        .getBaseAmount(DamageApplicationBucket.DN_RULE_BASE)
        );
    }

    @Test
    void blankSourceIdIsRejected() {
        DamageContextMutations mutations = mutations(
                new DamagePacketState(),
                new DamagePipelineResult(),
                DamagePhase.BASE_MODIFICATION
        );

        DamageMutationResult result = mutations.tryAddBaseDamage(
                DamageChannelRegistry.getUntyped(),
                2.0f,
                " "
        );

        assertEquals(DamageMutationResult.REJECTED_EMPTY_SOURCE_ID, result);
    }

    @Test
    void cancelDamageIsRejectedOutsideFinalOverride() {
        DamagePipelineResult pipelineResult = new DamagePipelineResult();
        DamageContextMutations mutations =
                mutations(new DamagePacketState(), pipelineResult, DamagePhase.BASE_MODIFICATION);

        DamageMutationResult result = mutations.tryCancelDamage("test/source");

        assertEquals(DamageMutationResult.REJECTED_WRONG_PHASE, result);
        assertFalse(pipelineResult.damageCancelled());
    }

    @Test
    void cancelDamageIsAllowedInFinalOverride() {
        DamagePipelineResult pipelineResult = new DamagePipelineResult();
        DamageContextMutations mutations =
                mutations(new DamagePacketState(), pipelineResult, DamagePhase.FINAL_OVERRIDE);

        DamageMutationResult result = mutations.tryCancelDamage("test/source");

        assertEquals(DamageMutationResult.APPLIED, result);
        assertTrue(pipelineResult.damageCancelled());
        assertEquals("test/source", pipelineResult.cancelSourceId());
    }

    private static DamageContextMutations mutations(
            DamagePacketState packet,
            DamagePipelineResult result,
            DamagePhase phase
    ) {
        DamagePhaseState phaseState = new DamagePhaseState();
        phaseState.setCurrentPhase(phase);

        return new DamageContextMutations(
                packet,
                new DamageCombatState(),
                result,
                phaseState,
                CombatTraceFactory.create(0)
        );
    }
}
