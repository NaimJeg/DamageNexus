package io.github.naimjeg.damagenexus.core.settlement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageSettlementPublicationGateTest {

    @Test
    void publicationCanBeClaimedExactlyOnce() {
        DamageSettlementPublicationGate gate =
                new DamageSettlementPublicationGate();

        assertFalse(gate.isClaimed());
        assertTrue(gate.claim());
        assertTrue(gate.isClaimed());
        assertFalse(gate.claim());
    }
}
