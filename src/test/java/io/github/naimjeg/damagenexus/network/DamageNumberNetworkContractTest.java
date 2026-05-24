package io.github.naimjeg.damagenexus.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNumberNetworkContractTest {

    @Test
    void commonPayloadRegistrationIsWiredExactlyOnceAndClientBound()
            throws IOException {
        String main = read("DamageNexus.java");
        String network = read("network/DamageNexusNetwork.java");

        assertEquals(
                1,
                count(main, "DamageNexusNetwork::registerPayloads")
        );
        assertTrue(network.contains("RegisterPayloadHandlersEvent"));
        assertTrue(network.contains("DamageNumberPayload.TYPE"));
        assertTrue(network.contains("DamageNumberPayload.STREAM_CODEC"));
        assertTrue(network.contains("playToClient"));
        assertFalse(network.contains("playToServer"));
        assertFalse(network.contains("playBidirectional"));
    }

    @Test
    void clientHandlerRegistrationRemainsPhysicalClientOnly()
            throws IOException {
        String clientHandler = read("client/ModClientHandler.java");
        String payloadHandler =
                read("client/damage/DamageNumberClientPayloadHandler.java");
        String network = read("network/DamageNexusNetwork.java");

        assertTrue(compact(clientHandler).contains(
                "DamageNumberClientPayloadHandler::onRegisterPayloadHandlers"
        ));
        assertTrue(payloadHandler.contains(
                "RegisterClientPayloadHandlersEvent"
        ));
        assertFalse(network.contains("RegisterClientPayloadHandlersEvent"));
    }

    @Test
    void broadcasterRoutesOnlyToTheLogicalPlayerAttacker()
            throws IOException {
        String broadcaster =
                read("presentation/damage/DamageNumberBroadcaster.java");

        assertTrue(broadcaster.contains("DamageSettledEvent"));
        assertTrue(broadcaster.contains("snapshot.appliedDamage()"));
        assertFalse(broadcaster.contains("snapshot.healthDamage()"));
        assertTrue(broadcaster.contains("snapshot.logicalAttacker()"));
        assertTrue(broadcaster.contains("snapshot.critical()"));
        assertTrue(broadcaster.contains("PacketDistributor.sendToPlayer"));
        assertFalse(broadcaster.contains("sendToAllPlayers"));
        assertFalse(broadcaster.contains("sendToPlayersTrackingEntity"));
        assertFalse(broadcaster.contains(
                "sendToPlayersTrackingEntityAndSelf"
        ));
    }

    @Test
    void broadcasterPresentsAppliedDamageNotObservedHealthLoss()
            throws IOException {
        String broadcaster =
                read("presentation/damage/DamageNumberBroadcaster.java");

        assertTrue(broadcaster.contains(
                "float damage = snapshot.appliedDamage();"
        ));
        assertFalse(broadcaster.contains("snapshot.healthDamage()"));
        assertFalse(broadcaster.contains("snapshot.resolvedDamage()"));
        assertFalse(broadcaster.contains(
                "snapshot.healthDamage() + snapshot.absorptionDamage()"
        ));
        assertTrue(broadcaster.contains("MIN_PRESENTED_DAMAGE"));
        assertFalse(broadcaster.contains("MIN_HEALTH_DAMAGE"));
        assertTrue(broadcaster.contains(
                "snapshot.status() != DamageSettlementStatus.APPLIED"
        ));
        assertTrue(broadcaster.contains("damage <= MIN_PRESENTED_DAMAGE"));
    }

    @Test
    void broadcasterRemainsDownstreamOfSettlementEvents()
            throws IOException {
        String broadcaster =
                read("presentation/damage/DamageNumberBroadcaster.java");

        assertFalse(broadcaster.contains("LivingIncomingDamageEvent"));
        assertFalse(broadcaster.contains("LivingDamageEvent"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(Path.of(
                "src/main/java/io/github/naimjeg/damagenexus",
                relative
        ));
    }

    private static int count(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) != -1) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
