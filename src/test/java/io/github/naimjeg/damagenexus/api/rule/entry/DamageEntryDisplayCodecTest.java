package io.github.naimjeg.damagenexus.api.rule.entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.display.DisplayText;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class DamageEntryDisplayCodecTest {
    @Test
    void newFieldRoundTripsAndEncodesCanonicalField() {
        DamageEntryDisplay value = new DamageEntryDisplay(
                Optional.of(DisplayText.literal("Name")),
                List.of(DisplayText.literal("Summary")),
                Optional.of(DisplayText.literal("Flavor")),
                true
        );
        JsonElement encoded = DamageEntryDisplay.CODEC
                .encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow();
        JsonObject json = encoded.getAsJsonObject();
        assertTrue(json.has("authored_summary"));
        assertFalse(json.has("tooltip"));
        assertEquals(value, DamageEntryDisplay.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow());
    }

    @Test
    void legacyTooltipDecodesIntoAuthoredSummary() {
        JsonObject json = base();
        json.add("tooltip", displayArray("Legacy line"));

        DamageEntryDisplay decoded = DamageEntryDisplay.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();
        assertFalse(json.has("authored_summary"));
        assertEquals(List.of(DisplayText.literal("Legacy line")),
                decoded.authoredSummary());
        assertEquals(decoded.authoredSummary(), decoded.tooltip());
    }

    @Test
    void sameNewAndLegacyFieldsDecodeWithoutError() {
        JsonObject json = base();
        JsonArray lines = displayArray("Shared line");
        json.add("authored_summary", lines.deepCopy());
        json.add("tooltip", lines.deepCopy());

        DamageEntryDisplay decoded = DamageEntryDisplay.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();
        assertEquals(List.of(DisplayText.literal("Shared line")),
                decoded.authoredSummary());
    }

    @Test
    void conflictingNewAndLegacyFieldsReturnAnError() {
        JsonObject json = base();
        json.add("authored_summary", displayArray("New line"));
        json.add("tooltip", displayArray("Old line"));

        DataResult<DamageEntryDisplay> result = DamageEntryDisplay.CODEC
                .parse(JsonOps.INSTANCE, json);
        assertTrue(result.error().isPresent());
    }

    @Test
    void missingSummaryFieldsDecodeToEmptyAuthoredSummary() {
        DamageEntryDisplay decoded = DamageEntryDisplay.CODEC
                .parse(JsonOps.INSTANCE, base())
                .getOrThrow();
        assertEquals(List.of(), decoded.authoredSummary());
    }

    @Test
    void lineLimitRejectsOverSizedAuthoredSummary() {
        JsonObject json = base();
        JsonArray lines = new JsonArray();
        for (int index = 0; index <= DamageRuleLimits.MAX_TOOLTIP_LINES; index++) {
            lines.add(DisplayText.CODEC
                    .encodeStart(JsonOps.INSTANCE, DisplayText.literal("line"))
                    .getOrThrow());
        }
        json.add("authored_summary", lines);

        assertTrue(DamageEntryDisplay.CODEC
                .parse(JsonOps.INSTANCE, json)
                .error()
                .isPresent());
    }

    @Test
    void deprecatedTooltipAccessorIsTheSameImmutableList() {
        DamageEntryDisplay value = new DamageEntryDisplay(
                Optional.empty(),
                List.of(DisplayText.literal("line")),
                Optional.empty(),
                false
        );
        assertSame(value.authoredSummary(), value.tooltip());
        assertThrows(UnsupportedOperationException.class, () ->
                value.tooltip().add(DisplayText.literal("extra")));
    }

    @Test
    void nullListBecomesEmptyAuthoredSummary() {
        DamageEntryDisplay value = new DamageEntryDisplay(
                Optional.empty(),
                null,
                Optional.empty(),
                false
        );
        assertEquals(List.of(), value.authoredSummary());
    }

    private static JsonObject base() {
        JsonObject json = new JsonObject();
        json.addProperty("show_rule_breakdown", true);
        return json;
    }

    private static JsonArray displayArray(String... lines) {
        JsonArray array = new JsonArray();
        for (String line : lines) {
            array.add(DisplayText.CODEC
                    .encodeStart(JsonOps.INSTANCE, DisplayText.literal(line))
                    .getOrThrow());
        }
        return array;
    }
}
