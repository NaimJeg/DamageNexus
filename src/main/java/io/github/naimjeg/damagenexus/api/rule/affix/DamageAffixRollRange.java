package io.github.naimjeg.damagenexus.api.rule.affix;

import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Experimental authoring-only roll range. Its key is not bound to any
 * operation field; reflection and implicit parameter replacement are not
 * supported.
 */
public record DamageAffixRollRange(
        Identifier key,
        float minValue,
        float maxValue,
        int decimalPlaces,
        Optional<Identifier> channelId,
        Optional<Identifier> preMultiplierBucketId
) {
    public DamageAffixRollRange {
        if (key == null) {
            throw new IllegalArgumentException("Damage affix roll key cannot be null");
        }

        if (!Float.isFinite(minValue)) {
            minValue = 0.0f;
        }

        if (!Float.isFinite(maxValue)) {
            maxValue = minValue;
        }

        if (maxValue < minValue) {
            float tmp = minValue;
            minValue = maxValue;
            maxValue = tmp;
        }

        decimalPlaces = Math.max(0, decimalPlaces);
        channelId = channelId == null ? Optional.empty() : channelId;
        preMultiplierBucketId = preMultiplierBucketId == null
                ? Optional.empty()
                : preMultiplierBucketId;
    }
}

