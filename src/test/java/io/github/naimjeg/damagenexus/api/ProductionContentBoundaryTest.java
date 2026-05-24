package io.github.naimjeg.damagenexus.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductionContentBoundaryTest {
    @Test
    void productionDataContainsNoRulesEntriesAffixesOrTemplates()
            throws Exception {
        Path data = Path.of("src/main/resources/data");
        List<String> executableDirectories = List.of(
                "damagenexus_rules",
                "damagenexus_entries",
                "damagenexus_affixes",
                "damagenexus_entry_templates",
                "damagenexus_affix_templates"
        );
        try (var files = Files.walk(data)) {
            List<String> paths = files.filter(Files::isRegularFile)
                    .map(path -> path.toString().replace('\\', '/'))
                    .toList();
            for (String directory : executableDirectories) {
                assertTrue(paths.stream().noneMatch(path ->
                                path.contains("/" + directory + "/")),
                        "Production executable definition found in "
                                + directory);
            }
            assertTrue(paths.stream().noneMatch(path ->
                            path.contains("/damage_type/")
                                    && !path.contains("/tags/damage_type/")),
                    "Production DamageType definition found");
        }
    }

    @Test
    void productionJavaRegistersNoGameplayRegistries() throws Exception {
        Path root = Path.of("src/main/java");
        StringBuilder source = new StringBuilder();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString()
                    .endsWith(".java")).toList()) {
                source.append(Files.readString(file)).append('\n');
            }
        }
        for (String registry : List.of(
                "Registries.ITEM",
                "Registries.BLOCK",
                "Registries.ENTITY_TYPE",
                "Registries.MOB_EFFECT",
                "Registries.ENCHANTMENT",
                "Registries.DAMAGE_TYPE")) {
            assertFalse(source.toString().contains(
                    "DeferredRegister.create(" + registry), registry);
        }
    }

    @Test
    void mainSourceTemplateRegistrationsAreGametestPropertyGuarded()
            throws Exception {
        for (String path : List.of(
                "src/main/java/io/github/naimjeg/damagenexus/core/request/DamageRequestGameTests.java",
                "src/main/java/io/github/naimjeg/damagenexus/core/security/DamageNexusItemSecurityGameTests.java")) {
            String source = Files.readString(Path.of(path));
            assertTrue(source.contains("damagenexus.gametest.runtime"), path);
            assertTrue(source.contains("registerEntryTemplate"), path);
            assertTrue(source.contains("Boolean.getBoolean"), path);
        }
    }
}
