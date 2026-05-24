package io.github.naimjeg.damagenexus.api.rule;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefinitionEnumCodecTest {
    @Test
    void allDefinitionEnumsRoundTripWithStableLowercaseNames() {
        roundTrips(DamageAffixRarity.CODEC, DamageAffixRarity.values());
        roundTrips(DamageAffixSlot.CODEC, DamageAffixSlot.values());
        roundTrips(DamageAffixStacking.CODEC, DamageAffixStacking.values());
        roundTrips(DamageEntrySlot.CODEC, DamageEntrySlot.values());
        roundTrips(DamageEntryStacking.CODEC, DamageEntryStacking.values());
    }

    @Test
    void unknownNamesReturnDataResultErrorsInsteadOfThrowing() {
        for (Codec<?> codec : List.of(
                DamageAffixRarity.CODEC,
                DamageAffixSlot.CODEC,
                DamageAffixStacking.CODEC,
                DamageEntrySlot.CODEC,
                DamageEntryStacking.CODEC)) {
            var result = assertDoesNotThrow(() -> codec.parse(
                    JsonOps.INSTANCE, new JsonPrimitive("not_a_real_value")));
            assertTrue(result.error().isPresent());
            assertTrue(result.error().orElseThrow().message()
                    .contains("not_a_real_value"));
        }
    }

    private static <T extends Enum<T>> void roundTrips(
            Codec<T> codec,
            T[] values
    ) {
        for (T value : values) {
            var encoded = codec.encodeStart(JsonOps.INSTANCE, value)
                    .getOrThrow();
            assertEquals(value, codec.parse(JsonOps.INSTANCE, encoded)
                    .getOrThrow());
            assertEquals(value.name().toLowerCase(), encoded.getAsString());
        }
    }
}
