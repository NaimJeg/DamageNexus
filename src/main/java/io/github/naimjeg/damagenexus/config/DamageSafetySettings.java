package io.github.naimjeg.damagenexus.config;

/** Immutable server-authoritative managed-damage admission limits. */
public record DamageSafetySettings(
        int maxRecursionDepth,
        int maxDerivedRequestsPerRoot,
        int maxManagedRequestsPerServerTick
) {
    public static final int DEFAULT_MAX_RECURSION_DEPTH = 5;
    public static final int DEFAULT_MAX_DERIVED_REQUESTS_PER_ROOT = 64;
    public static final int DEFAULT_MAX_MANAGED_REQUESTS_PER_SERVER_TICK = 2048;

    public static final int HARD_MAX_RECURSION_DEPTH = 64;
    public static final int HARD_MAX_DERIVED_REQUESTS_PER_ROOT = 65_536;
    public static final int HARD_MAX_MANAGED_REQUESTS_PER_SERVER_TICK =
            1_000_000;

    public DamageSafetySettings {
        requireRange(
                "maxRecursionDepth",
                maxRecursionDepth,
                HARD_MAX_RECURSION_DEPTH
        );
        requireRange(
                "maxDerivedRequestsPerRoot",
                maxDerivedRequestsPerRoot,
                HARD_MAX_DERIVED_REQUESTS_PER_ROOT
        );
        requireRange(
                "maxManagedRequestsPerServerTick",
                maxManagedRequestsPerServerTick,
                HARD_MAX_MANAGED_REQUESTS_PER_SERVER_TICK
        );
    }

    public static DamageSafetySettings defaults() {
        return new DamageSafetySettings(
                DEFAULT_MAX_RECURSION_DEPTH,
                DEFAULT_MAX_DERIVED_REQUESTS_PER_ROOT,
                DEFAULT_MAX_MANAGED_REQUESTS_PER_SERVER_TICK
        );
    }

    private static void requireRange(
            String name,
            int value,
            int maximum
    ) {
        if (value < 1 || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be in [1, " + maximum + "]"
            );
        }
    }
}
