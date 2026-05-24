package io.github.naimjeg.damagenexus.api.damage;

import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * A namespaced, strongly typed key for immutable request metadata.
 *
 * <p>The supported value families are deliberately closed so metadata cannot
 * retain Level, Entity, ItemStack, transaction, or other mutable runtime
 * objects.</p>
 */
public final class DamageMetadataKey<T> {

    public enum ValueType {
        BOOLEAN(Boolean.class),
        INTEGER(Integer.class),
        LONG(Long.class),
        DOUBLE(Double.class),
        STRING(String.class),
        IDENTIFIER(Identifier.class),
        UUID_VALUE(UUID.class);

        private final Class<?> javaType;

        ValueType(Class<?> javaType) {
            this.javaType = javaType;
        }

        Class<?> javaType() {
            return javaType;
        }
    }

    private final Identifier id;
    private final ValueType valueType;
    private final Class<T> javaType;

    private DamageMetadataKey(
            Identifier id,
            ValueType valueType,
            Class<T> javaType
    ) {
        this.id = Objects.requireNonNull(id, "Metadata key id must not be null");
        this.valueType = Objects.requireNonNull(
                valueType,
                "Metadata value type must not be null"
        );
        this.javaType = Objects.requireNonNull(
                javaType,
                "Metadata Java type must not be null"
        );
    }

    public static DamageMetadataKey<Boolean> booleanKey(Identifier id) {
        return new DamageMetadataKey<>(id, ValueType.BOOLEAN, Boolean.class);
    }

    public static DamageMetadataKey<Integer> integerKey(Identifier id) {
        return new DamageMetadataKey<>(id, ValueType.INTEGER, Integer.class);
    }

    public static DamageMetadataKey<Long> longKey(Identifier id) {
        return new DamageMetadataKey<>(id, ValueType.LONG, Long.class);
    }

    public static DamageMetadataKey<Double> doubleKey(Identifier id) {
        return new DamageMetadataKey<>(id, ValueType.DOUBLE, Double.class);
    }

    public static DamageMetadataKey<String> stringKey(Identifier id) {
        return new DamageMetadataKey<>(id, ValueType.STRING, String.class);
    }

    public static DamageMetadataKey<Identifier> identifierKey(Identifier id) {
        return new DamageMetadataKey<>(
                id,
                ValueType.IDENTIFIER,
                Identifier.class
        );
    }

    public static DamageMetadataKey<UUID> uuidKey(Identifier id) {
        return new DamageMetadataKey<>(id, ValueType.UUID_VALUE, UUID.class);
    }

    public Identifier id() {
        return id;
    }

    public ValueType valueType() {
        return valueType;
    }

    Class<T> javaType() {
        return javaType;
    }

    void validateValue(Object value) {
        if (value == null || !javaType.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Metadata value does not match key type: key="
                            + id
                            + " expected="
                            + javaType.getName()
            );
        }

        if (value instanceof Double number && !Double.isFinite(number)) {
            throw new IllegalArgumentException(
                    "Metadata double must be finite: key=" + id
            );
        }

        if (value instanceof String text
                && text.codePointCount(0, text.length())
                > DamageMetadata.MAX_STRING_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "Metadata string is too long: key=" + id
            );
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof DamageMetadataKey<?> key)) {
            return false;
        }

        return id.equals(key.id) && valueType == key.valueType;
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + valueType.hashCode();
    }

    @Override
    public String toString() {
        return "DamageMetadataKey[id=" + id + ", valueType=" + valueType + ']';
    }
}
