package io.github.naimjeg.damagenexus.core;

import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

public class DamageComponent {
    public final DamageChannel channel;

    private final float[] finalizedOffensiveByBucket =
            new float[DamageApplicationBucket.COUNT];

    private final float[] postMitigationByBucket =
            new float[DamageApplicationBucket.COUNT];

    private final float[] baseAmounts =
            new float[DamageApplicationBucket.COUNT];

    private PreMultiplierSet channelPreMultipliers = null;
    private PreMultiplierSet[] applicationPreMultipliers = null;

    private FloatArrayList postMultipliers = null;
    private FloatArrayList mitigationMultipliers = null;

    private float finalizedOffensiveAmount = 0.0f;
    private float postMitigationAmount = 0.0f;
    private float bypassMitigationAmount = 0.0f;

    private float temporaryResistanceRating = 0.0f;

    public DamageComponent(DamageChannel channel) {
        this.channel = channel;
    }

    /** Captures every mutable value owned by this channel component. */
    @ApiStatus.Internal
    public Checkpoint checkpoint() {
        return new Checkpoint(
                Arrays.copyOf(baseAmounts, baseAmounts.length),
                Arrays.copyOf(
                        finalizedOffensiveByBucket,
                        finalizedOffensiveByBucket.length
                ),
                Arrays.copyOf(
                        postMitigationByBucket,
                        postMitigationByBucket.length
                ),
                copy(channelPreMultipliers),
                copy(applicationPreMultipliers),
                copy(postMultipliers),
                copy(mitigationMultipliers),
                finalizedOffensiveAmount,
                postMitigationAmount,
                bypassMitigationAmount,
                temporaryResistanceRating
        );
    }

    /** Restores a checkpoint without changing this component's identity. */
    @ApiStatus.Internal
    public void restore(Checkpoint checkpoint) {
        if (checkpoint == null) {
            throw new IllegalArgumentException(
                    "Damage component checkpoint must not be null"
            );
        }

        System.arraycopy(
                checkpoint.baseAmounts,
                0,
                baseAmounts,
                0,
                baseAmounts.length
        );
        System.arraycopy(
                checkpoint.finalizedOffensiveByBucket,
                0,
                finalizedOffensiveByBucket,
                0,
                finalizedOffensiveByBucket.length
        );
        System.arraycopy(
                checkpoint.postMitigationByBucket,
                0,
                postMitigationByBucket,
                0,
                postMitigationByBucket.length
        );
        channelPreMultipliers = copy(checkpoint.channelPreMultipliers);
        applicationPreMultipliers = copy(
                checkpoint.applicationPreMultipliers
        );
        postMultipliers = copy(checkpoint.postMultipliers);
        mitigationMultipliers = copy(checkpoint.mitigationMultipliers);
        finalizedOffensiveAmount = checkpoint.finalizedOffensiveAmount;
        postMitigationAmount = checkpoint.postMitigationAmount;
        bypassMitigationAmount = checkpoint.bypassMitigationAmount;
        temporaryResistanceRating = checkpoint.temporaryResistanceRating;
    }

    private static PreMultiplierSet copy(PreMultiplierSet source) {
        return source == null ? null : source.copy();
    }

    private static PreMultiplierSet[] copy(PreMultiplierSet[] source) {
        if (source == null) {
            return null;
        }

        PreMultiplierSet[] copy = new PreMultiplierSet[source.length];
        for (int index = 0; index < source.length; index++) {
            copy[index] = copy(source[index]);
        }
        return copy;
    }

    private static FloatArrayList copy(FloatArrayList source) {
        return source == null ? null : new FloatArrayList(source);
    }

    @ApiStatus.Internal
    public static final class Checkpoint {

        private final float[] baseAmounts;
        private final float[] finalizedOffensiveByBucket;
        private final float[] postMitigationByBucket;
        private final PreMultiplierSet channelPreMultipliers;
        private final PreMultiplierSet[] applicationPreMultipliers;
        private final FloatArrayList postMultipliers;
        private final FloatArrayList mitigationMultipliers;
        private final float finalizedOffensiveAmount;
        private final float postMitigationAmount;
        private final float bypassMitigationAmount;
        private final float temporaryResistanceRating;

        private Checkpoint(
                float[] baseAmounts,
                float[] finalizedOffensiveByBucket,
                float[] postMitigationByBucket,
                PreMultiplierSet channelPreMultipliers,
                PreMultiplierSet[] applicationPreMultipliers,
                FloatArrayList postMultipliers,
                FloatArrayList mitigationMultipliers,
                float finalizedOffensiveAmount,
                float postMitigationAmount,
                float bypassMitigationAmount,
                float temporaryResistanceRating
        ) {
            this.baseAmounts = baseAmounts;
            this.finalizedOffensiveByBucket = finalizedOffensiveByBucket;
            this.postMitigationByBucket = postMitigationByBucket;
            this.channelPreMultipliers = channelPreMultipliers;
            this.applicationPreMultipliers = applicationPreMultipliers;
            this.postMultipliers = postMultipliers;
            this.mitigationMultipliers = mitigationMultipliers;
            this.finalizedOffensiveAmount = finalizedOffensiveAmount;
            this.postMitigationAmount = postMitigationAmount;
            this.bypassMitigationAmount = bypassMitigationAmount;
            this.temporaryResistanceRating = temporaryResistanceRating;
        }
    }

    public void addBase(float amount) {
        addBase(DamageApplicationBucket.DN_RULE_BASE, amount);
    }

    public void addPreMultiplier(int preMultiplierBucketId, float value) {
        if (channelPreMultipliers == null) {
            channelPreMultipliers = new PreMultiplierSet();
        }

        channelPreMultipliers.add(preMultiplierBucketId, value);
    }

    public void addPostMultiplier(float value) {
        if (postMultipliers == null) {
            postMultipliers = new FloatArrayList(4);
        }

        postMultipliers.add(value);
    }

    public void addMitigation(float reductionPercent) {
        if (mitigationMultipliers == null) {
            mitigationMultipliers = new FloatArrayList(4);
        }

        mitigationMultipliers.add(reductionPercent);
    }

    public void addTemporaryResistance(float amount) {
        if (!Float.isFinite(amount)) {
            throw new IllegalArgumentException("Temporary resistance must be finite");
        }
        double sum = (double) temporaryResistanceRating + amount;
        if (sum >= Float.MAX_VALUE) {
            temporaryResistanceRating = Float.MAX_VALUE;
        } else if (sum <= -Float.MAX_VALUE) {
            temporaryResistanceRating = -Float.MAX_VALUE;
        } else {
            temporaryResistanceRating = (float) sum;
        }
    }

    public float getTemporaryResistanceRating() {
        return temporaryResistanceRating;
    }

    public void calculateFinalOffensive(
            PreMultiplierSet globalPre,
            FloatArrayList globalPost
    ) {
        Arrays.fill(finalizedOffensiveByBucket, 0.0f);

        PreMultiplierBucketRegistry.requireFrozen();

        float mitigatedTotal = 0.0f;
        float trueTotal = 0.0f;

        for (DamageApplicationBucket bucket : DamageApplicationBucket.values()) {
            float amount = baseAmounts[bucket.ordinal()];

            if (amount == 0.0f) {
                continue;
            }

            amount = applyApplicationPreMultipliers(bucket, amount);
            amount = applyGenericPreMultipliers(globalPre, amount, bucket);

            if (postMultipliers != null && bucket.affectedByPostMultiplier()) {
                for (int i = 0; i < postMultipliers.size(); i++) {
                    amount *= 1.0f + postMultipliers.getFloat(i);
                }
            }

            if (globalPost != null && bucket.affectedByPostMultiplier()) {
                for (int i = 0; i < globalPost.size(); i++) {
                    amount *= 1.0f + globalPost.getFloat(i);
                }
            }

            if (!Float.isFinite(amount)) {
                amount = 0.0f;
            }

            amount = Math.max(0.0f, amount);

            finalizedOffensiveByBucket[bucket.ordinal()] = amount;

            if (bucket.affectedByMitigation()) {
                mitigatedTotal += amount;
            } else {
                trueTotal += amount;
            }
        }

        finalizedOffensiveAmount = Math.max(0.0f, mitigatedTotal + trueTotal);
        postMitigationAmount = Math.max(0.0f, mitigatedTotal);
        bypassMitigationAmount = Math.max(0.0f, trueTotal);
    }

    private float applyApplicationPreMultipliers(
            DamageApplicationBucket bucket,
            float amount
    ) {
        if (!bucket.affectedByApplicationPreMultiplier()
                || applicationPreMultipliers == null) {
            return amount;
        }

        PreMultiplierSet multipliers = applicationPreMultipliers[bucket.ordinal()];

        if (multipliers == null) {
            return amount;
        }

        return multipliers.apply(amount);
    }

    private float applyGenericPreMultipliers(
            PreMultiplierSet globalPre,
            float amount,
            DamageApplicationBucket bucket
    ) {
        PreMultiplierSet channelPre = bucket.affectedByChannelPreMultiplier()
                ? channelPreMultipliers
                : null;

        PreMultiplierSet global = bucket.affectedByGlobalPreMultiplier()
                ? globalPre
                : null;

        return PreMultiplierSet.applyCombined(amount, channelPre, global);
    }

    public float convertBaseTo(
            DamageComponent target,
            float ratio
    ) {
        if (target == null) {
            return 0.0f;
        }

        float safeRatio = Math.max(0.0f, Math.min(1.0f, ratio));

        if (safeRatio <= 0.0f) {
            return 0.0f;
        }

        float movedTotal = 0.0f;

        for (DamageApplicationBucket bucket : DamageApplicationBucket.values()) {
            int index = bucket.ordinal();

            float sourceAmount = baseAmounts[index];

            if (sourceAmount <= 0.0f) {
                continue;
            }

            float movedAmount = sourceAmount * safeRatio;

            if (movedAmount <= 0.0f) {
                continue;
            }

            baseAmounts[index] -= movedAmount;
            target.addBase(bucket, movedAmount);

            finalizedOffensiveByBucket[index] = movedAmount;

            movedTotal += movedAmount;
        }

        return movedTotal;
    }

    public void calculateFinalDefensive(FloatArrayList globalMitigations) {
        Arrays.fill(postMitigationByBucket, 0.0f);

        float mitigatedBeforeReduction = 0.0f;

        for (DamageApplicationBucket bucket : DamageApplicationBucket.values()) {
            int bucketIndex = bucket.ordinal();

            if (!bucket.affectedByMitigation()) {
                continue;
            }

            float offensive = finalizedOffensiveByBucket[bucketIndex];

            if (offensive <= 0.0f) {
                continue;
            }

            mitigatedBeforeReduction += offensive;
        }

        float mitigatedAfterReduction = mitigatedBeforeReduction;

        if (mitigationMultipliers != null) {
            for (int i = 0; i < mitigationMultipliers.size(); i++) {
                float reduction = mitigationMultipliers.getFloat(i);
                mitigatedAfterReduction *= Math.max(0.0f, 1.0f - reduction);
            }
        }

        if (globalMitigations != null) {
            for (int i = 0; i < globalMitigations.size(); i++) {
                float reduction = globalMitigations.getFloat(i);
                mitigatedAfterReduction *= Math.max(0.0f, 1.0f - reduction);
            }
        }

        if (!Float.isFinite(mitigatedAfterReduction)) {
            mitigatedAfterReduction = 0.0f;
        }

        mitigatedAfterReduction = Math.max(0.0f, mitigatedAfterReduction);

        float mitigationRatio;

        if (mitigatedBeforeReduction > 0.0f) {
            mitigationRatio = mitigatedAfterReduction / mitigatedBeforeReduction;
        } else {
            mitigationRatio = 1.0f;
        }

        if (!Float.isFinite(mitigationRatio)) {
            mitigationRatio = 0.0f;
        }

        mitigationRatio = Math.max(0.0f, mitigationRatio);

        float total = 0.0f;

        for (DamageApplicationBucket bucket : DamageApplicationBucket.values()) {
            int bucketIndex = bucket.ordinal();

            float offensive = finalizedOffensiveByBucket[bucketIndex];

            if (offensive <= 0.0f) {
                continue;
            }

            float bucketPostMitigation;

            if (bucket.affectedByMitigation()) {
                bucketPostMitigation = offensive * mitigationRatio;
            } else {
                bucketPostMitigation = offensive;
            }

            if (!Float.isFinite(bucketPostMitigation)) {
                bucketPostMitigation = 0.0f;
            }

            bucketPostMitigation = Math.max(0.0f, bucketPostMitigation);

            postMitigationByBucket[bucketIndex] = bucketPostMitigation;
            total += bucketPostMitigation;
        }

        if (!Float.isFinite(total)) {
            total = 0.0f;
        }

        postMitigationAmount = Math.max(0.0f, total);
    }

    public float getBaseAmount() {
        float total = 0.0f;

        for (float amount : baseAmounts) {
            total += amount;
        }

        return total;
    }

    public float getBaseAmount(DamageApplicationBucket bucket) {
        if (bucket == null) {
            return 0.0f;
        }

        return baseAmounts[bucket.ordinal()];
    }

    public boolean hasBaseDamage() {
        return getBaseAmount() > 0.0f;
    }

    public boolean hasFinalizedOffensiveDamage() {
        return finalizedOffensiveAmount > 0.0f;
    }

    public boolean hasPostMitigationDamage() {
        return postMitigationAmount > 0.0f;
    }

    /**
     * Returns true if this component currently represents a real damage channel
     * at any pipeline stage.
     * <p>
     * This is intentionally stage-agnostic:
     * - before offensive finalization: base amounts are authoritative
     * - after offensive finalization: finalizedOffensiveAmount is authoritative
     * - after defensive calculation: postMitigationAmount is authoritative
     */
    public boolean hasAnyPositiveDamage() {
        return getBaseAmount() > 0.0f
                || finalizedOffensiveAmount > 0.0f
                || postMitigationAmount > 0.0f;
    }

    public float getCurrentBestKnownAmount() {
        if (postMitigationAmount > 0.0f) {
            return postMitigationAmount;
        }

        if (finalizedOffensiveAmount > 0.0f) {
            return finalizedOffensiveAmount;
        }

        return Math.max(0.0f, getBaseAmount());
    }

    public void addBase(
            DamageApplicationBucket bucket,
            float amount
    ) {
        if (bucket == null) {
            bucket = DamageApplicationBucket.DN_RULE_BASE;
        }

        baseAmounts[bucket.ordinal()] += amount;
    }

    public void addApplicationPreMultiplier(
            DamageApplicationBucket bucket,
            int preMultiplierBucketId,
            float value
    ) {
        if (bucket == null) {
            return;
        }

        if (applicationPreMultipliers == null) {
            applicationPreMultipliers = new PreMultiplierSet[DamageApplicationBucket.COUNT];
        }

        int bucketIndex = bucket.ordinal();

        if (applicationPreMultipliers[bucketIndex] == null) {
            applicationPreMultipliers[bucketIndex] = new PreMultiplierSet();
        }

        applicationPreMultipliers[bucketIndex].add(preMultiplierBucketId, value);
    }

    public float getFinalizedOffensiveAmount(DamageApplicationBucket bucket) {
        if (bucket == null) {
            return 0.0f;
        }

        return finalizedOffensiveByBucket[bucket.ordinal()];
    }

    public float getPostMitigationAmount(DamageApplicationBucket bucket) {
        if (bucket == null) {
            return 0.0f;
        }

        return postMitigationByBucket[bucket.ordinal()];
    }

    public float getFinalizedOffensiveAmount() {
        return finalizedOffensiveAmount;
    }

    public float getPostMitigationAmount() {
        return postMitigationAmount;
    }
}
