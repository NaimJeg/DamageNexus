package io.github.naimjeg.damagenexus.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCodecs;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.PreMultiplierBucketReferencingOperation;
import io.github.naimjeg.damagenexus.api.rule.RuleTraceIds;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record AddGlobalPreMultiplierOperation(
        Optional<Identifier> preMultiplierBucketId,
        float value
) implements DamageRuleOperation, PreMultiplierBucketReferencingOperation {

    public static final MapCodec<AddGlobalPreMultiplierOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.PRE_MULTIPLIER_BUCKET_ID
                            .optionalFieldOf("pre_multiplier_bucket")
                            .forGetter(AddGlobalPreMultiplierOperation::preMultiplierBucketId),

                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddGlobalPreMultiplierOperation::value)
            ).apply(instance, AddGlobalPreMultiplierOperation::new));


    public AddGlobalPreMultiplierOperation {
        if (preMultiplierBucketId == null) {
            preMultiplierBucketId = Optional.empty();
        }
    }

    @Override
    public Identifier type() {
        return DamageNexusOperationIds.ADD_GLOBAL_PRE_MULTIPLIER;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        int bucketId =
                DamageOperationPreMultiplierBuckets.resolveOrGeneric(
                        preMultiplierBucketId
                );

        return ctx.tryAddGlobalPreMultiplier(
                bucketId,
                value,
                RuleTraceIds.ADD_GLOBAL_PRE_MULTIPLIER
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(
                DamagePhase.TYPE_SCALING,
                DamagePhase.CRITICAL_HIT,
                DamagePhase.CONDITIONAL_MULTI,
                DamagePhase.GLOBAL_ADJUSTMENT
        );
    }

    @Override
    public List<Identifier> referencedPreMultiplierBuckets() {
        return preMultiplierBucketId
                .map(List::of)
                .orElseGet(List::of);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
