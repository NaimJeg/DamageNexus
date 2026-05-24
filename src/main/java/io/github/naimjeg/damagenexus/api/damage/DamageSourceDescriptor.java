package io.github.naimjeg.damagenexus.api.damage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;

/**
 * Stable description used to create a short-lived vanilla DamageSource at
 * submission time.
 */
public record DamageSourceDescriptor(
        ResourceKey<DamageType> damageType,
        Optional<Vec3> sourcePosition
) {
    public DamageSourceDescriptor {
        damageType = Objects.requireNonNull(
                damageType,
                "Damage type key must not be null"
        );
        sourcePosition = sourcePosition == null
                ? Optional.empty()
                : sourcePosition.map(position -> new Vec3(
                        position.x,
                        position.y,
                        position.z
                ));

        sourcePosition.ifPresent(position -> {
            if (!Double.isFinite(position.x)
                    || !Double.isFinite(position.y)
                    || !Double.isFinite(position.z)) {
                throw new IllegalArgumentException(
                        "Damage source position must be finite"
                );
            }
        });
    }

    public static DamageSourceDescriptor of(
            ResourceKey<DamageType> damageType
    ) {
        return new DamageSourceDescriptor(damageType, Optional.empty());
    }

    public static DamageSourceDescriptor positioned(
            ResourceKey<DamageType> damageType,
            Vec3 sourcePosition
    ) {
        return new DamageSourceDescriptor(
                damageType,
                Optional.of(Objects.requireNonNull(
                        sourcePosition,
                        "Damage source position must not be null"
                ))
        );
    }

    public static DamageSourceDescriptor from(DamageSource source) {
        Objects.requireNonNull(source, "Damage source must not be null");

        return tryFrom(source).orElseThrow(() -> new IllegalArgumentException(
                "Damage source type is not registry-backed"
        ));
    }

    /**
     * Safely describes a registry-backed source, or returns empty for a
     * direct/unregistered holder. No synthetic identifier is generated.
     */
    public static Optional<DamageSourceDescriptor> tryFrom(
            DamageSource source
    ) {
        Objects.requireNonNull(source, "Damage source must not be null");

        Optional<ResourceKey<DamageType>> key = source.typeHolder()
                .unwrapKey();
        if (key.isEmpty()) {
            return Optional.empty();
        }
        Vec3 position = source.sourcePositionRaw();

        return Optional.of(position == null
                ? of(key.orElseThrow())
                : positioned(key.orElseThrow(), position));
    }
}
