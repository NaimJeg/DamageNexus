package io.github.naimjeg.damagenexus.diagnostics.logging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ClientForwardingRemovalTest {

    @Test
    void productionSourceContainsNoClientDiagnosticForwarding() throws IOException {
        Path root = Path.of("src", "main", "java");
        List<String> forbidden = List.of(
                "client" + "Forward",
                "Client" + "DebugLog",
                "debug" + "_forward",
                "forward" + "AlreadyAccepted",
                "should" + "Forward",
                "OPTED" + "_IN"
        );

        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                String source = Files.readString(file);
                for (String token : forbidden) {
                    assertFalse(
                            source.contains(token),
                            () -> file + " still contains " + token
                    );
                }
            }
        }
    }
}
