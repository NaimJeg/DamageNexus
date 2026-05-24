package io.github.naimjeg.damagenexus.api.damage;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable, type-safe metadata carried by a damage request. */
public final class DamageMetadata {

    public static final int MAX_ENTRIES = 64;
    public static final int MAX_STRING_CODE_POINTS = 2_048;

    private static final DamageMetadata EMPTY =
            new DamageMetadata(Map.of());

    private final Map<DamageMetadataKey<?>, Object> values;

    private DamageMetadata(Map<DamageMetadataKey<?>, Object> values) {
        this.values = Collections.unmodifiableMap(
                new LinkedHashMap<>(values)
        );
    }

    public static DamageMetadata empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> Optional<T> get(DamageMetadataKey<T> key) {
        Objects.requireNonNull(key, "Metadata key must not be null");
        Object value = values.get(key);

        return value == null
                ? Optional.empty()
                : Optional.of(key.javaType().cast(value));
    }

    public boolean contains(DamageMetadataKey<?> key) {
        return key != null && values.containsKey(key);
    }

    public Set<DamageMetadataKey<?>> keys() {
        return values.keySet();
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Builder toBuilder() {
        return new Builder(values);
    }

    public static final class Builder {

        private final Map<DamageMetadataKey<?>, Object> values =
                new LinkedHashMap<>();
        private final Map<Identifier, DamageMetadataKey.ValueType> idTypes =
                new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(Map<DamageMetadataKey<?>, Object> initial) {
            for (Map.Entry<DamageMetadataKey<?>, Object> entry
                    : initial.entrySet()) {
                putUntyped(entry.getKey(), entry.getValue());
            }
        }

        public <T> Builder put(DamageMetadataKey<T> key, T value) {
            return putUntyped(key, value);
        }

        public Builder putAll(DamageMetadata metadata) {
            Objects.requireNonNull(metadata, "Metadata must not be null");

            for (Map.Entry<DamageMetadataKey<?>, Object> entry
                    : metadata.values.entrySet()) {
                putUntyped(entry.getKey(), entry.getValue());
            }

            return this;
        }

        public DamageMetadata build() {
            if (values.isEmpty()) {
                return DamageMetadata.empty();
            }

            return new DamageMetadata(values);
        }

        private Builder putUntyped(
                DamageMetadataKey<?> key,
                Object value
        ) {
            Objects.requireNonNull(key, "Metadata key must not be null");
            key.validateValue(value);

            DamageMetadataKey.ValueType existingType = idTypes.get(key.id());
            if (existingType != null && existingType != key.valueType()) {
                throw new IllegalArgumentException(
                        "Metadata id cannot be used with multiple types: "
                                + key.id()
                );
            }

            if (!values.containsKey(key) && values.size() >= MAX_ENTRIES) {
                throw new IllegalArgumentException(
                        "Damage metadata exceeds maximum entries: "
                                + MAX_ENTRIES
                );
            }

            idTypes.put(key.id(), key.valueType());
            values.put(key, value);
            return this;
        }
    }
}
