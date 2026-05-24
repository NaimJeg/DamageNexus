package io.github.naimjeg.damagenexus.event.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageTransactionHandlerPolicyContractTest {

    @Test
    void preAlwaysCapturesFormalSettlementBeforeDiagnosticsPolicyGate()
            throws IOException {
        String source = source("PreDamageTransactionHandler");
        int formalCapture = source.indexOf(
                "DamageSettlementTracker.capturePre(event)"
        );
        int policyGate = source.indexOf(
                "DamageSourcePolicy.shouldManage(event.getSource())"
        );
        int promotion = source.indexOf(
                "DamageNexusTransactionTracker.promoteIncomingCandidate"
        );

        assertTrue(formalCapture >= 0);
        assertTrue(formalCapture < policyGate);
        assertTrue(policyGate < promotion);
    }

    @Test
    void postFormalCaptureIsIndependentAndPolicyPrecedesQueueAccess()
            throws IOException {
        String source = source("PostDamageHandler");
        int formalCapture = source.indexOf(
                "DamageSettlementTracker.capturePost(event)"
        );
        int policyGate = source.indexOf(
                "DamageSourcePolicy.shouldManage(event.getSource())"
        );
        int queuePoll = source.indexOf(
                "DamageNexusTransactionTracker.pollMatchingPostTrackable"
        );

        assertTrue(formalCapture >= 0);
        assertTrue(policyGate >= 0);
        assertTrue(policyGate < queuePoll);
    }

    @Test
    void sourcePolicyExcludesAllThreeUnmanagedSourceClasses()
            throws IOException {
        String source = Files.readString(Path.of(
                "src", "main", "java", "io", "github", "naimjeg",
                "damagenexus", "core", "pipeline", "DamageSourcePolicy.java"
        ));

        assertTrue(source.contains("unwrapKey().isEmpty()"));
        assertTrue(source.contains("BYPASSES_DAMAGENEXUS"));
        assertTrue(source.contains("BYPASSES_INVULNERABILITY"));
    }

    private static String source(String className) throws IOException {
        return Files.readString(Path.of(
                "src", "main", "java", "io", "github", "naimjeg",
                "damagenexus", "event", "neoforge", className + ".java"
        ));
    }
}
