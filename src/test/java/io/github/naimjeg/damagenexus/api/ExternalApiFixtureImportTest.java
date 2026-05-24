package io.github.naimjeg.damagenexus.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExternalApiFixtureImportTest {
    @Test
    void fixtureImportsNoDamageNexusCoreInternalOrRegistryTypes() throws Exception {
        Path root = Path.of(
                "src/test/java/io/github/naimjeg/damagenexus/externalapi");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString()
                    .endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains(
                        "io.github.naimjeg.damagenexus.core"), file.toString());
                assertFalse(source.contains(
                        "io.github.naimjeg.damagenexus.internal"), file.toString());
                assertFalse(source.contains(
                        "io.github.naimjeg.damagenexus.registry"), file.toString());
                assertFalse(source.contains(
                        "io.github.naimjeg.damagenexus.diagnostics"), file.toString());
            }
        }
    }
}
