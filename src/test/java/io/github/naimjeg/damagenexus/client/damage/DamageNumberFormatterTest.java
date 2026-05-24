package io.github.naimjeg.damagenexus.client.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageNumberFormatterTest {

    @Test
    void formatsSmallWholeAndDecimalValues() {
        assertEquals("0", DamageNumberFormatter.format(0.0F));
        assertEquals("1", DamageNumberFormatter.format(1.0F));
        assertEquals("1", DamageNumberFormatter.format(1.04F));
        assertEquals("1.1", DamageNumberFormatter.format(1.06F));
        assertEquals("17", DamageNumberFormatter.format(17.0F));
        assertEquals("17.3", DamageNumberFormatter.format(17.25F));
        assertEquals("99.9", DamageNumberFormatter.format(99.94F));
    }

    @Test
    void roundsLargeValuesToWholeNumbers() {
        assertEquals("100", DamageNumberFormatter.format(100.0F));
        assertEquals("127", DamageNumberFormatter.format(127.3F));
        assertEquals("1043", DamageNumberFormatter.format(1042.8F));
    }

    @Test
    void handlesInvalidValuesDefensively() {
        assertEquals("0", DamageNumberFormatter.format(Float.NaN));
        assertEquals("0", DamageNumberFormatter.format(Float.POSITIVE_INFINITY));
        assertEquals("0", DamageNumberFormatter.format(Float.NEGATIVE_INFINITY));
    }
}
