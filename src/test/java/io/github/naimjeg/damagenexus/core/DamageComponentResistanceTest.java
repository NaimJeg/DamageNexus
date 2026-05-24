package io.github.naimjeg.damagenexus.core;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DamageComponentResistanceTest {
    @Test
    void finiteTemporaryRatingsSaturateWithoutChangingSignOrBecomingZero() {
        DamageComponent positive = new DamageComponent(
                new DamageChannel(DamageChannel.FIRE_ID, 1));
        positive.addTemporaryResistance(Float.MAX_VALUE);
        positive.addTemporaryResistance(Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, positive.getTemporaryResistanceRating());

        DamageComponent negative = new DamageComponent(
                new DamageChannel(DamageChannel.FIRE_ID, 1));
        negative.addTemporaryResistance(-Float.MAX_VALUE);
        negative.addTemporaryResistance(-Float.MAX_VALUE);
        assertEquals(-Float.MAX_VALUE, negative.getTemporaryResistanceRating());
        assertThrows(IllegalArgumentException.class,
                () -> positive.addTemporaryResistance(Float.POSITIVE_INFINITY));
    }
}
