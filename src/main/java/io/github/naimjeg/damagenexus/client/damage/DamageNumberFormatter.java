package io.github.naimjeg.damagenexus.client.damage;

import java.util.Locale;

public final class DamageNumberFormatter {

    private static final float INTEGER_EPSILON = 0.05F;
    private static final float DECIMAL_LARGE_THRESHOLD = 100.0F;

    private DamageNumberFormatter() {
    }

    public static String format(float damage) {
        if (!Float.isFinite(damage)) {
            return "0";
        }

        long rounded = Math.round(damage);
        if (damage >= DECIMAL_LARGE_THRESHOLD) {
            return Long.toString(rounded);
        }

        if (Math.abs(damage - rounded) < INTEGER_EPSILON) {
            return Long.toString(rounded);
        }

        float oneDecimal = Math.round(damage * 10.0F) / 10.0F;
        return String.format(Locale.ROOT, "%.1f", oneDecimal);
    }
}
