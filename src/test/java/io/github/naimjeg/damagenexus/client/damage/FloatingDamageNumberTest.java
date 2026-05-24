package io.github.naimjeg.damagenexus.client.damage;

import io.github.naimjeg.damagenexus.network.payload.DamageNumberPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatingDamageNumberTest {

    @Test
    void popScaleFollowsOvershootThenSettles() {
        FloatingDamageNumber number = from(1L, false);

        assertEquals(0.65F, number.popScale(0.0F), 1.0E-6F);

        tick(number, 3);
        assertEquals(1.25F, number.popScale(0.0F), 1.0E-6F);

        tick(number, 3);
        assertEquals(1.0F, number.popScale(0.0F), 1.0E-6F);
    }

    @Test
    void criticalScaleIsTwentyPercentLarger() {
        FloatingDamageNumber normal = from(1L, false);
        FloatingDamageNumber critical = from(1L, true);
        tick(normal, 6);
        tick(critical, 6);

        assertEquals(
                normal.worldScale(0.0F) * 1.2F,
                critical.worldScale(0.0F),
                1.0E-6F
        );
    }

    @Test
    void fadeStartsAtSixteenAndEndsAtLifetime() {
        FloatingDamageNumber number = from(1L, false);
        tick(number, 16);
        assertEquals(1.0F, number.alpha(0.0F), 1.0E-6F);

        tick(number, 4);
        assertEquals(0.5F, number.alpha(0.0F), 1.0E-6F);

        tick(number, 4);
        assertEquals(0.0F, number.alpha(0.0F), 1.0E-6F);
        assertTrue(number.expired());
    }

    @Test
    void numbersRiseOverTheirLifetime() {
        FloatingDamageNumber number = from(7L, false);
        double startY = number.renderY(0.0F);

        tick(number, 24);
        double endY = number.renderY(0.0F);

        assertTrue(endY > startY);
        assertEquals(
                number.anchorY() + number.offsetY() + 0.45D,
                endY,
                1.0E-5D
        );
    }

    @Test
    void verticalRiseIsMonotonic() {
        FloatingDamageNumber number = from(7L, false);
        double previousY = number.renderY(0.0F);

        for (int age = 1; age <= 24; age++) {
            number.tick();
            double currentY = number.renderY(0.0F);
            assertTrue(currentY >= previousY);
            previousY = currentY;
        }
    }

    @Test
    void deterministicOffsetsStayWithinBoundsAndDifferAcrossIds() {
        FloatingDamageNumber first = from(1L, false);
        boolean anyDifferent = false;

        for (long id = 0L; id < 16L; id++) {
            FloatingDamageNumber number = from(id, false);
            assertTrue(number.offsetX() >= -0.30F);
            assertTrue(number.offsetX() <= 0.30F);
            assertTrue(number.offsetY() >= 0.0F);
            assertTrue(number.offsetY() <= 0.18F);
            assertTrue(number.offsetZ() >= -0.15F);
            assertTrue(number.offsetZ() <= 0.15F);

            if (Float.compare(number.offsetX(), first.offsetX()) != 0
                    || Float.compare(number.offsetY(), first.offsetY()) != 0
                    || Float.compare(number.offsetZ(), first.offsetZ()) != 0) {
                anyDifferent = true;
            }
        }

        assertTrue(anyDifferent);
        assertEquals(
                first.offsetX(),
                from(1L, false).offsetX()
        );
        assertEquals(
                first.offsetY(),
                from(1L, false).offsetY()
        );
        assertEquals(
                first.offsetZ(),
                from(1L, false).offsetZ()
        );
    }

    @Test
    void lifetimeMatchesTwentyFourTicks() {
        FloatingDamageNumber number = from(1L, false);
        assertFalse(number.expired());
        assertEquals(24, FloatingDamageNumber.LIFETIME_TICKS);
        tick(number, 24);
        assertTrue(number.expired());
    }

    private static FloatingDamageNumber from(long id, boolean critical) {
        return FloatingDamageNumber.from(
                new DamageNumberPayload(
                        id,
                        10.0D,
                        20.0D,
                        30.0D,
                        17.5F,
                        critical
                )
        );
    }

    private static void tick(FloatingDamageNumber number, int ticks) {
        for (int i = 0; i < ticks; i++) {
            number.tick();
        }
    }
}
