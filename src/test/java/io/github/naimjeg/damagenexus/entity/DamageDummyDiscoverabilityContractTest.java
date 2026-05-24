package io.github.naimjeg.damagenexus.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source/resource-level contract for the damage dummy discoverability work:
 * the {@code entity.damagenexus.damage_dummy} translation key, its en_us and
 * zh_cn values, the separation from the block identity, and the absence of
 * custom {@code getName}/{@code getDisplayName} overrides or constructor-time
 * custom names.
 *
 * <p>Registry-backed assertions (for example
 * {@code ModEntityTypes.DAMAGE_DUMMY.get().getDescriptionId()}) cannot run in
 * a plain JUnit JVM because the mod's DeferredRegister is only populated
 * during a real game bootstrap; that contract is covered by the
 * {@code damage_dummy_name_translation} GameTest instead.</p>
 */
class DamageDummyDiscoverabilityContractTest {

    private static final String ENTITY_KEY =
            "entity.damagenexus.damage_dummy";
    private static final String BLOCK_KEY =
            "block.damagenexus.damage_dummy";

    @Test
    void entityTranslationKeyIsPresentInBothLocales() throws IOException {
        JsonObject en = readLang("en_us");
        JsonObject zh = readLang("zh_cn");

        assertTrue(en.has(ENTITY_KEY), "en_us must define " + ENTITY_KEY);
        assertTrue(zh.has(ENTITY_KEY), "zh_cn must define " + ENTITY_KEY);
        assertEquals("Damage Dummy", en.get(ENTITY_KEY).getAsString());
        assertEquals("伤害测试木人", zh.get(ENTITY_KEY).getAsString());
    }

    @Test
    void entityKeyRemainsDistinctFromBlockIdentity() throws IOException {
        JsonObject en = readLang("en_us");
        JsonObject zh = readLang("zh_cn");

        assertEquals("Damage Dummy Pedestal",
                en.get(BLOCK_KEY).getAsString());
        assertEquals("伤害假人基座",
                zh.get(BLOCK_KEY).getAsString());
        assertNotEquals(en.get(ENTITY_KEY).getAsString(),
                en.get(BLOCK_KEY).getAsString());
        assertNotEquals(zh.get(ENTITY_KEY).getAsString(),
                zh.get(BLOCK_KEY).getAsString());
    }

    @Test
    void damageDummyEntityReliesOnVanillaNameSemantics() throws IOException {
        String source = readMainSource("entity/DamageDummyEntity.java");
        assertFalse(source.contains("getName("),
                "DamageDummyEntity must not override getName()");
        assertFalse(source.contains("getDisplayName("),
                "DamageDummyEntity must not override getDisplayName()");
        assertFalse(source.contains("setCustomName("),
                "DamageDummyEntity must not assign a custom name");
        assertFalse(source.contains("Component.literal("),
                "DamageDummyEntity must not use literal name components");
    }

    @Test
    void entityTypeRegistrationUsesExpectedPath() throws IOException {
        String source = readMainSource("registry/ModEntityTypes.java");
        assertTrue(source.contains("registerEntityType("),
                "ModEntityTypes must register via registerEntityType");
        assertTrue(source.contains("\"damage_dummy\""),
                "ModEntityTypes must register the damage_dummy path");
        assertTrue(readMainSource("DamageNexus.java")
                        .contains("MODID = \"damagenexus\""),
                "mod id must be damagenexus");
    }

    @Test
    void creativeTabHandlerAppendsOnlyTheExistingBlockItem()
            throws IOException {
        String source = readMainSource("registry/ModCreativeTabContents.java");
        assertTrue(source.contains("CreativeModeTabs.FUNCTIONAL_BLOCKS"),
                "creative handler must target the vanilla Functional Blocks tab");
        assertTrue(source.contains("ModItems.DAMAGE_DUMMY.get()"),
                "creative handler must reuse the existing BlockItem");
        assertTrue(source.contains("event.accept("),
                "creative handler must append through event.accept");
        assertFalse(source.contains("insertBefore"),
                "creative handler must not use insertBefore");
        assertFalse(source.contains("insertAfter"),
                "creative handler must not use insertAfter");
        assertFalse(source.contains("putBefore"),
                "creative handler must not use putBefore");
        assertFalse(source.contains("putAfter"),
                "creative handler must not use putAfter");
    }

    private static JsonObject readLang(String locale) throws IOException {
        Path path = Path.of(
                "src/main/resources/assets/damagenexus/lang",
                locale + ".json"
        );
        String source = StandardCharsets.UTF_8.decode(
                java.nio.ByteBuffer.wrap(Files.readAllBytes(path))
        ).toString();
        assertFalse(source.contains("\uFFFD"),
                locale + " must be strict UTF-8 without replacement chars");
        return JsonParser.parseString(source).getAsJsonObject();
    }

    private static String readMainSource(String relative)
            throws IOException {
        Path path = Path.of(
                "src/main/java/io/github/naimjeg/damagenexus",
                relative
        );
        return Files.readString(path);
    }
}
