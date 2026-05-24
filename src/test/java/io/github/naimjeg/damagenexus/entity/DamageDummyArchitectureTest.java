package io.github.naimjeg.damagenexus.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Targeted source-level guard for the damage dummy architecture.
 *
 * <p>Runtime behavior (registry-driven universal attribute attachment,
 * editing, damage, persistence) is covered by {@code DamageDummyGameTests},
 * including the GameTest-only sentinel attribute that the production code
 * never names. Source-text assertions are deliberately limited to the one
 * structural guarantee that cannot be observed at runtime: no production
 * dummy/entity registration class may special-case the sentinel.</p>
 */
class DamageDummyArchitectureTest {

    private static final String SENTINEL_ID =
            "damage_dummy_test_attribute";

    @Test
    void productionRegistrationClassesDoNotReferenceSentinel()
            throws IOException {
        assertNoSentinelReference("entity/DamageDummyEntity.java");
        assertNoSentinelReference("registry/ModEntityAttributes.java");
        assertNoSentinelReference("registry/ModEntityTypes.java");
    }

    private static void assertNoSentinelReference(String relative)
            throws IOException {
        Path path = Path.of(
                "src/main/java/io/github/naimjeg/damagenexus",
                relative
        );
        String source = Files.readString(path);
        assertFalse(
                source.contains(SENTINEL_ID),
                relative + " must not special-case the GameTest-only sentinel"
        );
    }
}
