package io.github.naimjeg.damagenexus.api.rule.affix;

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
class DamageAffixDisplayCodecTest {
    @Test
    void newFieldRoundTripsAndEncodesCanonicalField() {
        DamageAffixDisplay value = new DamageAffixDisplay(
                Optional.of(DisplayText.literal("Name")),
                List.of(DisplayText.literal("Summary")),
                Optional.of(DisplayText.literal("Flavor")),
                true
        );
        JsonElement encoded = DamageAffixDisplay.CODEC
                .encodeStart(JsonOps.INSTANCE, value)
                .getOrThrow();
        JsonObject json = encoded.getAsJsonObject();
        assertTrue(json.has("authored_summary"));
        assertFalse(json.has("tooltip"));
        assertEquals(value, DamageAffixDisplay.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow());
    }

    @Test
    void legacyTooltipDecodesIntoAuthoredSummary() {
        JsonObject json = base();
        json.add("tooltip", displayArray("Legacy line"));

        DamageAffixDisplay decoded = DamageAffixDisplay.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();
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

        DamageAffixDisplay decoded = DamageAffixDisplay.CODEC
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

        DataResult<DamageAffixDisplay> result = DamageAffixDisplay.CODEC
                .parse(JsonOps.INSTANCE, json);
        assertTrue(result.error().isPresent());
    }

    @Test
    void missingSummaryFieldsDecodeToEmptyAuthoredSummary() {
        DamageAffixDisplay decoded = DamageAffixDisplay.CODEC
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

        assertTrue(DamageAffixDisplay.CODEC
                .parse(JsonOps.INSTANCE, json)
                .error()
                .isPresent());
    }

    @Test
    void deprecatedTooltipAccessorIsTheSameImmutableList() {
        DamageAffixDisplay value = new DamageAffixDisplay(
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
        DamageAffixDisplay value = new DamageAffixDisplay(
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
