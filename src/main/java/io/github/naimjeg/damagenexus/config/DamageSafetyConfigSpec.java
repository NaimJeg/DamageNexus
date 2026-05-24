package io.github.naimjeg.damagenexus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common-config definitions for Phase 4 damage admission safety. */
public final class DamageSafetyConfigSpec {
    public static ModConfigSpec.IntValue MAX_RECURSION_DEPTH;
    public static ModConfigSpec.IntValue MAX_DERIVED_REQUESTS_PER_ROOT;
    public static ModConfigSpec.IntValue MAX_MANAGED_REQUESTS_PER_SERVER_TICK;

    private DamageSafetyConfigSpec() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("damageSafety");

        MAX_RECURSION_DEPTH = builder
                .comment(
                        "Maximum explicit DamageLineage recursion depth.",
                        "Root depth is 0; a value of 5 permits depths 0 through 5.",
                        "Default: 5"
                )
                .defineInRange(
                        "maxRecursionDepth",
                        DamageSafetySettings.DEFAULT_MAX_RECURSION_DEPTH,
                        1,
                        DamageSafetySettings.HARD_MAX_RECURSION_DEPTH
                );

        MAX_DERIVED_REQUESTS_PER_ROOT = builder
                .comment(
                        "Maximum admitted child requests shared by one root chain.",
                        "The root request itself does not consume this count.",
                        "Default: 64"
                )
                .defineInRange(
                        "maxDerivedRequestsPerRoot",
                        DamageSafetySettings
                                .DEFAULT_MAX_DERIVED_REQUESTS_PER_ROOT,
                        1,
                        DamageSafetySettings
                                .HARD_MAX_DERIVED_REQUESTS_PER_ROOT
                );

        MAX_MANAGED_REQUESTS_PER_SERVER_TICK = builder
                .comment(
                        "Maximum managed damage admissions per MinecraftServer tick.",
                        "All dimensions and both public/native damage share the limit.",
                        "Default: 2048"
                )
                .defineInRange(
                        "maxManagedRequestsPerServerTick",
                        DamageSafetySettings
                                .DEFAULT_MAX_MANAGED_REQUESTS_PER_SERVER_TICK,
                        1,
                        DamageSafetySettings
                                .HARD_MAX_MANAGED_REQUESTS_PER_SERVER_TICK
                );

        builder.pop();
    }

    static DamageSafetySettings bake() {
        return new DamageSafetySettings(
                MAX_RECURSION_DEPTH.get(),
                MAX_DERIVED_REQUESTS_PER_ROOT.get(),
                MAX_MANAGED_REQUESTS_PER_SERVER_TICK.get()
        );
    }
}
