package io.github.naimjeg.damagenexus.gametest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GameTestRegistrationCodecContractTest {

    private static final List<String> TEST_IDS = List.of(
            "security_boundaries",
            "creative_item_security_service",
            "creative_packet_ingress_security",
            "public_damage_settlement",
            "registry_dependency_readiness",
            "settlement_event_repost_safety"
    );

    @Test
    void productionSourcesRegisterSixUniqueFunctionInstances()
            throws IOException {
        List<String> sources = List.of(
                source("command", "DamageNexusSecurityGameTests"),
                source("core/security", "DamageNexusItemSecurityGameTests"),
                source("core/request", "DamageRequestGameTests")
        );
        String combined = String.join("\n", sources);

        assertFalse(combined.contains("extends GameTestInstance"));
        assertFalse(combined.contains(
                "return FunctionGameTestInstance.CODEC"
        ));
        assertEquals(6, occurrences(
                combined,
                "new FunctionGameTestInstance("
        ));
        assertEquals(6, occurrences(combined, "event.registerTest("));
        assertEquals(6, occurrences(
                combined,
                "GameTestCodecVerifier.verifyFunctionInstance("
        ));
        assertEquals(6, occurrences(
                combined,
                "[DamageNexus] Executing GameTest {}"
        ));
        assertEquals(3, occurrences(
                combined,
                "GameTestHooks.isGametestEnabled()"
        ));

        for (String id : TEST_IDS) {
            assertEquals(
                    1,
                    occurrences(combined, "id(\"" + id + "\"),\n"
                            + "                new FunctionGameTestInstance("),
                    "test instance must be registered once: " + id
            );
            assertEquals(
                    2,
                    occurrences(combined, "\"" + id + "\""),
                    "test and function IDs must match: " + id
            );
        }

        assertEquals(6, TEST_IDS.stream().distinct().count());
    }

    private static String source(String packagePath, String className)
            throws IOException {
        return Files.readString(Path.of(
                "src",
                "main",
                "java",
                "io",
                "github",
                "naimjeg",
                "damagenexus",
                packagePath,
                className + ".java"
        )).replace("\r\n", "\n");
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
