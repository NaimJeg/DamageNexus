package io.github.naimjeg.damagenexus.api.critical;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

/** Describes the authoritative path that produced the final critical result. */
public enum CriticalDecisionOutcome {
    UNRESOLVED,
    SUPPRESSED,
    FORCED,
    VANILLA_MELEE,
    VANILLA_PROJECTILE,
    ATTRIBUTE_CHANCE,
    DEFAULT_NON_CRITICAL,
    INELIGIBLE;

    public static final Codec<CriticalDecisionOutcome> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                for (CriticalDecisionOutcome outcome : values()) {
                    if (outcome.serializedName().equals(value)) {
                        return DataResult.success(outcome);
                    }
                }
                return DataResult.error(() -> "Unknown critical outcome: " + value);
            },
            CriticalDecisionOutcome::serializedName
    );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "critical_outcome.damagenexus." + serializedName();
    }
}
