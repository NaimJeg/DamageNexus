package io.github.naimjeg.damagenexus.api.client.phrase;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A slot is deliberately identity-based. Recreating a slot with the same name
 * does not grant access to a schema owned by another mod.
 */
public final class PhraseSlot<T extends PhraseValue> {
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]{0,47}");

    private final String name;
    private final Class<T> valueType;
    private final boolean required;

    private PhraseSlot(String name, Class<T> valueType, boolean required) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid phrase slot name: " + name);
        }
        this.name = name;
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.required = required;
    }

    public static <T extends PhraseValue> PhraseSlot<T> required(
            String name,
            Class<T> valueType
    ) {
        return new PhraseSlot<>(name, valueType, true);
    }

    public static <T extends PhraseValue> PhraseSlot<T> optional(
            String name,
            Class<T> valueType
    ) {
        return new PhraseSlot<>(name, valueType, false);
    }

    public String name() {
        return name;
    }

    public Class<T> valueType() {
        return valueType;
    }

    public boolean required() {
        return required;
    }
}
