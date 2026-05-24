package io.github.naimjeg.damagenexus.network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageDummyNetworkArchitectureTest {

    private static final Path MAIN = Path.of(
            "src/main/java/io/github/naimjeg/damagenexus"
    );

    @Test
    void commonAndDedicatedServerClassesNeverReferenceClientScreen()
            throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(path ->
                    path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains(
                            java.io.File.separator + "client"
                                    + java.io.File.separator
                    )).toList()) {
                assertFalse(
                        Files.readString(file).contains("DamageDummyScreen"),
                        file + " must not load a physical-client Screen"
                );
            }
        }
    }

    @Test
    void serverValidationExplicitlyRejectsEveryBatchHazard()
            throws IOException {
        String service = Files.readString(MAIN.resolve(
                "entity/DamageDummyAttributeService.java"
        ));
        String handler = Files.readString(MAIN.resolve(
                "network/DamageDummyAttributePayloadHandler.java"
        ));

        assertTrue(service.contains("edits == null"));
        assertTrue(service.contains("MAX_ATTRIBUTES"));
        assertTrue(service.contains("!seen.add"));
        assertTrue(service.contains("!Double.isFinite"));
        assertTrue(service.contains("holder == null"));
        assertTrue(service.contains("instance == null"));
        assertTrue(service.contains("sanitizeValue"));
        assertTrue(service.indexOf("for (ValidatedEdit edit : validated)")
                > service.indexOf("for (DamageDummyAttributeEdit edit : edits)"));
        assertTrue(handler.contains("menu.containerId != payload.containerId()"));
        assertTrue(handler.contains("!menu.anchorPos().equals(payload.anchorPos())"));
        assertTrue(handler.contains("menu.stillValid(player)"));
        assertTrue(handler.contains("resolveManagedDummy"));
    }
}
