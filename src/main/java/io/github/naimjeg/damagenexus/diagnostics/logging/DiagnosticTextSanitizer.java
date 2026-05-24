package io.github.naimjeg.damagenexus.diagnostics.logging;

public final class DiagnosticTextSanitizer {

    public static final int MAX_LOG_CODE_POINTS = 512;

    private DiagnosticTextSanitizer() {
    }

    public static String sanitizeLine(String value) {
        return sanitizeLine(value, MAX_LOG_CODE_POINTS);
    }

    public static String sanitizeLine(
            String value,
            int maximumCodePoints
    ) {
        if (value == null) {
            return "<null>";
        }

        if (maximumCodePoints <= 0) {
            return "";
        }

        StringBuilder output = new StringBuilder(
                Math.min(value.length(), maximumCodePoints)
        );
        int emitted = 0;
        int index = 0;

        while (index < value.length() && emitted < maximumCodePoints) {
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);

            if (emitted == maximumCodePoints - 1
                    && index < value.length()) {
                output.append('\u2026');
                emitted++;
                break;
            }

            output.appendCodePoint(isUnsafe(codePoint) ? ' ' : codePoint);
            emitted++;
        }

        return output.toString().strip();
    }

    public static Object[] sanitizeArguments(Object... arguments) {
        if (arguments == null || arguments.length == 0) {
            return arguments == null ? new Object[0] : arguments;
        }

        Object[] sanitized = arguments.clone();

        for (int index = 0; index < sanitized.length; index++) {
            Object value = sanitized[index];

            if (value instanceof Throwable) {
                continue;
            }

            if (value instanceof CharSequence sequence) {
                sanitized[index] = sanitizeLine(sequence.toString());
            }
        }

        return sanitized;
    }

    private static boolean isUnsafe(int codePoint) {
        int type = Character.getType(codePoint);

        return Character.isISOControl(codePoint)
                || type == Character.FORMAT
                || codePoint == 0x061C
                || codePoint == 0x200E
                || codePoint == 0x200F
                || codePoint >= 0x202A && codePoint <= 0x202E
                || codePoint >= 0x2066 && codePoint <= 0x2069
                || codePoint == 0xFEFF;
    }
}
