package io.github.naimjeg.damagenexus.client.tooltip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class TooltipTestLanguage extends Language implements AutoCloseable {
    private final Language previous;
    private final Map<String, String> values;

    private TooltipTestLanguage(Map<String, String> values) {
        this.previous = Language.getInstance();
        this.values = Map.copyOf(values);
        Language.inject(this);
    }

    static TooltipTestLanguage install(String locale) throws IOException {
        Path path = Path.of(
                "src/main/resources/assets/damagenexus/lang/"
                        + locale + ".json"
        );
        JsonObject json = JsonParser.parseString(
                Files.readString(path, StandardCharsets.UTF_8)
        ).getAsJsonObject();
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getAsString());
        }
        return new TooltipTestLanguage(values);
    }

    static TooltipTestLanguage installWith(
            String locale,
            String key,
            String value
    ) throws IOException {
        return installWith(locale, Map.of(key, value));
    }

    static TooltipTestLanguage installWith(
            String locale,
            Map<String, String> additions
    ) throws IOException {
        Path path = Path.of(
                "src/main/resources/assets/damagenexus/lang/"
                        + locale + ".json"
        );
        JsonObject json = JsonParser.parseString(
                Files.readString(path, StandardCharsets.UTF_8)
        ).getAsJsonObject();
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getAsString());
        }
        values.putAll(additions);
        return new TooltipTestLanguage(values);
    }

    @Override
    public String getOrDefault(String elementId, String defaultValue) {
        return values.getOrDefault(elementId, defaultValue);
    }

    @Override
    public boolean has(String elementId) {
        return values.containsKey(elementId);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return false;
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText logicalOrderText) {
        return previous.getVisualOrder(logicalOrderText);
    }

    @Override
    public Map<String, String> getLanguageData() {
        return values;
    }

    @Override
    public void close() {
        Language.inject(previous);
    }
}
