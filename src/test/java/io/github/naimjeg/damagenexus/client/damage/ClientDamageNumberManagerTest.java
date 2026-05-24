package io.github.naimjeg.damagenexus.client.damage;

import io.github.naimjeg.damagenexus.network.payload.DamageNumberPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientDamageNumberManagerTest {

    @AfterEach
    void clearManager() {
        ClientDamageNumberManager.clear();
    }

    @Test
    void activeCapacityDropsOldestNumbers() {
        int spawnCount = ClientDamageNumberManager.MAX_ACTIVE + 5;
        for (long id = 0L; id < spawnCount; id++) {
            ClientDamageNumberManager.spawn(payload(id));
        }

        assertEquals(
                ClientDamageNumberManager.MAX_ACTIVE,
                ClientDamageNumberManager.active().size()
        );
        assertEquals(
                5L,
                ClientDamageNumberManager.active().getFirst().damageId()
        );
        assertEquals(
                spawnCount - 1L,
                ClientDamageNumberManager.active()
                        .getLast()
                        .damageId()
        );
    }

    private static DamageNumberPayload payload(long id) {
        return new DamageNumberPayload(
                id,
                0.0D,
                0.0D,
                0.0D,
                10.0F,
                false
        );
    }
}
