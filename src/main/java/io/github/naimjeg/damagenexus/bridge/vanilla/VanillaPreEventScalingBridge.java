package io.github.naimjeg.damagenexus.bridge.vanilla;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class VanillaPreEventScalingBridge {

    private static final float EPSILON = 0.0001f;

    private VanillaPreEventScalingBridge() {
    }

    public static boolean canApply(
            DamageNexusContext ctx,
            PreEventDeltaKind expectedKind,
            boolean allowPositiveRatio
    ) {
        if (!ctx.shouldRebuildVanillaPreEventDelta()) {
            return false;
        }

        VanillaDamageCapture.OffensiveSnapshot snapshot =
                ctx.getVanillaSnapshot();

        if (snapshot == null) {
            return false;
        }

        VanillaDamageCapture.PreEventDelta delta =
                snapshot.preEventDelta();

        if (delta.kind() != expectedKind) {
            return false;
        }

        float ratio = delta.ratio();

        if (!Float.isFinite(ratio)) {
            return false;
        }

        if (Math.abs(ratio - 1.0f) <= EPSILON) {
            return false;
        }

        /*
         * Player attack scaling currently means vanilla attack-cooldown /
         * attack-strength penalty. It must not absorb positive bonuses such as
         * crit, enchantment, mace, spear, or unknown modded scaling.
         */
        return allowPositiveRatio || !(ratio > 1.0f + EPSILON);
    }

    public static void applyApplicationPreMultiplier(
            DamageNexusContext ctx,
            PreEventDeltaKind expectedKind,
            DamageApplicationBucket applicationBucket,
            int preMultiplierBucket,
            Identifier preMultiplierBucketId,
            String traceId,
            boolean allowPositiveRatio
    ) {
        if (!canApply(ctx, expectedKind, allowPositiveRatio)) {
            return;
        }

        float value = multiplierDelta(ctx);

        if (value == 0.0f) {
            return;
        }

        recordApplicationPreMultiplier(
                ctx,
                applicationBucket,
                preMultiplierBucket,
                preMultiplierBucketId,
                value,
                traceId
        );
    }

    public static void applyApplicationPreMultiplierToAll(
            DamageNexusContext ctx,
            PreEventDeltaKind expectedKind,
            int preMultiplierBucket,
            Identifier preMultiplierBucketId,
            String traceId,
            boolean allowPositiveRatio,
            DamageApplicationBucket... applicationBuckets
    ) {
        if (!canApply(ctx, expectedKind, allowPositiveRatio)) {
            return;
        }

        float value = multiplierDelta(ctx);

        if (value == 0.0f) {
            return;
        }

        if (applicationBuckets == null) {
            return;
        }

        for (DamageApplicationBucket applicationBucket : applicationBuckets) {
            recordApplicationPreMultiplier(
                    ctx,
                    applicationBucket,
                    preMultiplierBucket,
                    preMultiplierBucketId,
                    value,
                    traceId
            );
        }
    }

    private static void recordApplicationPreMultiplier(
            DamageNexusContext ctx,
            DamageApplicationBucket applicationBucket,
            int preMultiplierBucket,
            Identifier preMultiplierBucketId,
            float value,
            String traceId
    ) {
        if (applicationBucket == null) {
            return;
        }

        DamageMutationResult result = ctx.tryAddApplicationPreMultiplier(
                applicationBucket,
                preMultiplierBucket,
                value,
                traceId
        );

        ctx.contributions().record(
                result,
                () -> VanillaContributionDescriptors.vanillaMultiplier(
                        contributionId(traceId, applicationBucket),
                        DamagePhase.GLOBAL_ADJUSTMENT,
                        ctx.getInitialChannel().id(),
                        applicationBucket,
                        preMultiplierBucketId,
                        value,
                        traceId
                )
        );
    }

    private static float multiplierDelta(DamageNexusContext ctx) {
        VanillaDamageCapture.OffensiveSnapshot snapshot =
                ctx.getVanillaSnapshot();

        if (snapshot == null) {
            return 0.0f;
        }

        float ratio = snapshot.preEventDelta().ratio();

        if (!Float.isFinite(ratio)) {
            return 0.0f;
        }

        float value = ratio - 1.0f;

        return Math.abs(value) > EPSILON ? value : 0.0f;
    }

    private static Identifier contributionId(
            String traceId,
            DamageApplicationBucket bucket
    ) {
        String normalizedTrace = traceId == null || traceId.isBlank()
                ? "unknown"
                : traceId;

        int namespaceSeparator = normalizedTrace.indexOf(':');

        if (namespaceSeparator >= 0) {
            normalizedTrace = normalizedTrace.substring(namespaceSeparator + 1);
        }

        normalizedTrace = normalizedTrace
                .replace(':', '_')
                .replace('/', '_')
                .replace(' ', '_')
                .toLowerCase(Locale.ROOT);

        return DamageNexusIds.id(
                "vanilla_pre_event/"
                        + normalizedTrace
                        + "/"
                        + bucket.name().toLowerCase(Locale.ROOT)
        );
    }
}
