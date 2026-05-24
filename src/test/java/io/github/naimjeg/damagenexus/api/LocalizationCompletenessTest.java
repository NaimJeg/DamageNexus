package io.github.naimjeg.damagenexus.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.naimjeg.damagenexus.api.critical.*;
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
    private static final Pattern FORMAT = Pattern.compile("%(?:\\d+\\$)?[sdf]");

    @Test
    void languageFilesAreStrictUtf8FlatJsonWithMatchingCompleteKeys() throws Exception {
        Language en = read("en_us");
        Language zh = read("zh_cn");
        assertEquals(en.values.keySet(), zh.values.keySet());
        assertFalse(en.source.contains("\uFFFD"));
        assertFalse(zh.source.contains("\uFFFD"));
        for (String key : en.values.keySet()) {
            assertEquals(formatCount(en.values.get(key)),
                    formatCount(zh.values.get(key)), key);
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
        for (Identifier id : identifiers(DamageNexusConditionIds.class)) {
            assertTrue(en.values.containsKey("condition.damagenexus." + id.getPath()),
                    id.toString());
        }
        for (Identifier id : identifiers(DamageNexusOperationIds.class)) {
            assertTrue(en.values.containsKey("operation.damagenexus.normal." + id.getPath()),
                    id.toString());
            assertTrue(en.values.containsKey("operation.damagenexus.detail." + id.getPath()),
                    id.toString());
        }
        for (Field field : DamageNexusAttributes.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                String path = ((net.minecraft.resources.ResourceKey<?>) field.get(null))
                        .identifier().getPath();
                assertTrue(en.values.containsKey("attribute.name.damagenexus." + path), path);
            }
        }
        for (String key : List.of(
                "tooltip.damagenexus.template.entry_reference",
                "tooltip.damagenexus.template.affix_reference",
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

    private static int formatCount(String text) {
        int count = 0;
        Matcher matcher = FORMAT.matcher(text);
        while (matcher.find()) count++;
        return count;
    }

    private static boolean requiresTranslation(String key) {
        return key.startsWith("attribute.")
                || key.startsWith("condition.")
                || key.startsWith("operation.damagenexus.normal.")
                || key.startsWith("operation.damagenexus.detail.")
                || key.startsWith("damage_request_kind.")
                || key.startsWith("damagenexus.damage_request.failure.")
                || key.startsWith("critical_")
                || key.startsWith("diagnostic.");
    }

    private record Language(String source, Map<String, String> values) {}
}
