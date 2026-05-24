package io.github.naimjeg.damagenexus.api.critical;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

/** A monotonic request to leave, force, or suppress critical damage. */
public enum CriticalDecision {
    DEFAULT,
    FORCE_CRITICAL,
    SUPPRESS_CRITICAL;

    public static final Codec<CriticalDecision> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                for (CriticalDecision decision : values()) {
                    if (decision.serializedName().equals(value)) {
                        return DataResult.success(decision);
                    }
                }
                return DataResult.error(() -> "Unknown critical decision: " + value);
            },
            CriticalDecision::serializedName
    );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "critical_decision.damagenexus." + serializedName();
    }
}
