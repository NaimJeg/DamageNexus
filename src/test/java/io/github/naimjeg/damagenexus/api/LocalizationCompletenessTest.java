package io.github.naimjeg.damagenexus.client.tooltip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.naimjeg.damagenexus.api.critical.*;
import io.github.naimjeg.damagenexus.api.DamageNexusAttributes;
import io.github.naimjeg.damagenexus.api.client.phrase.PhraseForm;
import io.github.naimjeg.damagenexus.api.client.phrase.RulePhraseRegistry;
import io.github.naimjeg.damagenexus.api.damage.DamageFailureReason;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class LocalizationCompletenessTest {
    private static final Pattern KEY = Pattern.compile(
            "(?m)^\\s*\"((?:\\\\.|[^\"])*)\"\\s*:");
    private static final Pattern FORMAT = Pattern.compile("%(\\d+)\\$[sdf]");
    private static final Pattern UNINDEXED_FORMAT = Pattern.compile("%(?!\\d+\\$)[sdf]");

    @Test
    void languageFilesAreStrictUtf8FlatJsonWithMatchingCompleteKeys() throws Exception {
        Language en = read("en_us");
        Language zh = read("zh_cn");
        assertEquals(en.values.keySet(), zh.values.keySet());
        assertFalse(en.source.contains("\uFFFD"));
        assertFalse(zh.source.contains("\uFFFD"));
        for (String key : en.values.keySet()) {
            assertEquals(formatIndexes(en.values.get(key)),
                    formatIndexes(zh.values.get(key)), key);
            assertFalse(UNINDEXED_FORMAT.matcher(en.values.get(key)).find(), key);
            assertFalse(UNINDEXED_FORMAT.matcher(zh.values.get(key)).find(), key);
            if (requiresTranslation(key)
                    && en.values.get(key).matches(".*[A-Za-z]{2,}.*")) {
                assertNotEquals(en.values.get(key), zh.values.get(key), key);
            }
        }

        for (DamageFailureReason value : DamageFailureReason.values()) {
            assertTrue(en.values.containsKey(value.translationKey()));
        }
        for (DamageRequestKind value : DamageRequestKind.values()) {
            assertTrue(en.values.containsKey(value.translationKey()));
        }
        for (CriticalDecision value : CriticalDecision.values()) {
            assertTrue(en.values.containsKey(value.translationKey()));
        }
        for (CriticalDecisionOutcome value : CriticalDecisionOutcome.values()) {
            assertTrue(en.values.containsKey(value.translationKey()));
        }
        for (CriticalDecisionContributionResult value
                : CriticalDecisionContributionResult.values()) {
            assertTrue(en.values.containsKey(value.translationKey()));
        }
        RulePhraseRegistry registry = new RulePhraseRegistry();
        DamageNexusRulePhraseBootstrap.register(registry);
        for (var schema : registry.schemas()) {
            Set<Integer> expected = new LinkedHashSet<>();
            for (int index = 1; index <= schema.slots().size(); index++) {
                expected.add(index);
            }
            for (var variant : schema.variants()) {
                for (PhraseForm form : PhraseForm.values()) {
                    String key = schema.type().translationKey(variant, form);
                    assertTrue(en.values.containsKey(key), key);
                    assertEquals(expected, formatIndexes(en.values.get(key)), key);
                }
            }
        }
        assertEquals(Set.of(1, 2), formatIndexes(
                en.values.get("rule_sentence.damagenexus.conditional.single")
        ));
        for (net.minecraft.world.entity.MobCategory category
                : net.minecraft.world.entity.MobCategory.values()) {
            assertTrue(en.values.containsKey(
                    "mob_category.damagenexus." + category.getSerializedName()
            ));
        }
        assertEquals("Affix", en.values.get("source_kind.damagenexus.affix"));
        assertEquals("词缀", zh.values.get("source_kind.damagenexus.affix"));
        assertEquals("Entry", en.values.get("source_kind.damagenexus.entry"));
        assertEquals("条目", zh.values.get("source_kind.damagenexus.entry"));

        Pattern commandKey = Pattern.compile("\"(command\\.damagenexus\\.[a-z0-9_.]+)\"");
        try (var paths = Files.walk(Path.of(
                "src/main/java/io/github/naimjeg/damagenexus/command"
        ))) {
            for (Path source : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = commandKey.matcher(Files.readString(source));
                while (matcher.find()) {
                    assertTrue(en.values.containsKey(matcher.group(1)),
                            source + ": " + matcher.group(1));
                }
            }
        }
        for (Field field : DamageNexusAttributes.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                String path = ((net.minecraft.resources.ResourceKey<?>) field.get(null))
                        .identifier().getPath();
                assertTrue(en.values.containsKey("attribute.name.damagenexus." + path), path);
            }
        }
        for (String key : List.of(
                "template.damagenexus.entry_reference",
                "template.damagenexus.affix_reference",
                "template.damagenexus.revision",
                "template.damagenexus.server_execution_ready",
                "template.damagenexus.validated_channel_revision",
                "diagnostic.damagenexus.template.unresolved",
                "diagnostic.damagenexus.template.reload_rejected",
                "diagnostic.damagenexus.registry.dependency_incompatible")) {
            assertTrue(en.values.containsKey(key), key);
        }
    }

    private static Language read(String locale) throws Exception {
        Path path = Path.of("src/main/resources/assets/damagenexus/lang/"
                + locale + ".json");
        byte[] bytes = Files.readAllBytes(path);
        String source = StandardCharsets.UTF_8.newDecoder().decode(
                java.nio.ByteBuffer.wrap(bytes)).toString();
        Matcher matcher = KEY.matcher(source);
        Set<String> keys = new LinkedHashSet<>();
        int count = 0;
        while (matcher.find()) {
            count++;
            assertTrue(keys.add(matcher.group(1)),
                    "Duplicate JSON key: " + matcher.group(1));
        }
        JsonObject json = JsonParser.parseString(source).getAsJsonObject();
        assertEquals(count, json.size(), "Language JSON must be a flat object");
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            assertTrue(entry.getValue().isJsonPrimitive());
            values.put(entry.getKey(), entry.getValue().getAsString());
        }
        return new Language(source, values);
    }

    private static List<Identifier> identifiers(Class<?> owner) throws Exception {
        List<Identifier> ids = new ArrayList<>();
        for (Field field : owner.getFields()) {
            if (field.getType() == Identifier.class
                    && Modifier.isStatic(field.getModifiers())) {
                ids.add((Identifier) field.get(null));
            }
        }
        return ids;
    }

    private static Set<Integer> formatIndexes(String text) {
        Set<Integer> indexes = new LinkedHashSet<>();
        Matcher matcher = FORMAT.matcher(text);
        while (matcher.find()) indexes.add(Integer.parseInt(matcher.group(1)));
        return indexes;
    }

    private static boolean requiresTranslation(String key) {
        return key.startsWith("attribute.")
                || key.startsWith("rule_phrase.")
                || key.startsWith("damage_request_kind.")
                || key.startsWith("damagenexus.damage_request.failure.")
                || key.startsWith("critical_")
                || key.startsWith("diagnostic.");
    }

    private record Language(String source, Map<String, String> values) {}
}
