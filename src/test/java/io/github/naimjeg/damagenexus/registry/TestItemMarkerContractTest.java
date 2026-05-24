package io.github.naimjeg.damagenexus.registry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestItemMarkerContractTest {

    @Test
    void testItemIsRegisteredAsPersistentBooleanMetadata()
            throws IOException {
        String components = read(
                "registry/ModDataComponents.java"
        );

        assertEquals(1, count(components, "\"test_item\""));
        assertTrue(compact(components).contains(
                "COMPONENTS.register(\"test_item\","
        ));
        assertTrue(components.contains(".persistent(Codec.BOOL)"));
    }

    @Test
    void factoryIdentityUsesOnlyTheMarkerComponent()
            throws IOException {
        String factory = read("command/test/TestItemFactory.java");

        assertTrue(factory.contains("ModDataComponents.TEST_ITEM"));
        assertTrue(factory.contains("Boolean.TRUE.equals"));
        assertTrue(factory.contains(
                "stack.get(ModDataComponents.TEST_ITEM.get())"
        ));

        String identityMethod = between(
                factory,
                "public static boolean isTestItem",
                "public static ItemStack physicalScalingSword"
        );
        assertFalse(identityMethod.contains("CUSTOM_NAME"));
        assertFalse(identityMethod.contains("DAMAGE_ENTRIES"));
        assertFalse(identityMethod.contains("DAMAGE_AFFIXES"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(Path.of(
                "src/main/java/io/github/naimjeg/damagenexus",
                relative
        ));
    }

    private static String between(
            String source,
            String startToken,
            String endToken
    ) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        return source.substring(start, end);
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
