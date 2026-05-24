package io.github.naimjeg.damagenexus.api.display;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.client.tooltip.DisplayTextResolver;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DisplayTextValidationTest {
    private Language previous;

    @BeforeEach
    void installLanguage() {
        previous = Language.getInstance();
        Language.inject(new MapLanguage(Map.of(
                "example.name", "Translated %1$s"
        ), previous));
    }

    @AfterEach
    void restoreLanguage() {
        Language.inject(previous);
    }

    @Test
    void literalAndTranslatableAreExclusiveAndCodecRejectsIllegalCombinations() {
        assertCodecError("{}");
        assertCodecError("{\"translate\":\"example.name\",\"text\":\"both\"}");
        assertCodecError("{\"text\":\"literal\",\"args\":[\"x\"]}");
        assertCodecError("{\"text\":\"literal\",\"fallback\":\"x\"}");
        assertCodecError("{\"translate\":\"Bad Key\"}");
    }

    @Test
    void newlinesControlsAndFormattingCodesAreRejected() {
        assertCodecError("{\"text\":\"line\\nline\"}");
        assertCodecError("{\"text\":\"bad\\u0007value\"}");
        assertCodecError("{\"text\":\"bad\\u00a7cvalue\"}");
        assertThrows(IllegalArgumentException.class, () ->
                DisplayText.literal("line\rline"));
        assertThrows(IllegalArgumentException.class, () ->
                DisplayText.translatable("example.name", "bad\u200barg"));
    }

    @Test
    void fallbackAndArgumentsResolveThroughTheSingleResolver() {
        DisplayText translated = DisplayText.translatableWithFallback(
                "missing.name", "Fallback %1$s", "value"
        );
        DisplayText present = DisplayText.translatableWithFallback(
                "example.name", "Fallback %1$s", "value"
        );
        assertEquals("Fallback value", DisplayTextResolver.resolve(translated).getString());
        assertEquals("Translated value", DisplayTextResolver.resolve(present).getString());
        assertEquals(
                DisplayTextResolver.resolve(translated),
                DisplayTextResolver.resolveOptional(Optional.of(translated)).orElseThrow()
        );
    }

    private static void assertCodecError(String json) {
        assertTrue(DisplayText.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString(json)
        ).error().isPresent(), json);
    }

    private static final class MapLanguage extends Language {
        private final Map<String, String> values;
        private final Language visualDelegate;

        private MapLanguage(Map<String, String> values, Language visualDelegate) {
            this.values = values;
            this.visualDelegate = visualDelegate;
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
            return visualDelegate.getVisualOrder(logicalOrderText);
        }
    }
}
