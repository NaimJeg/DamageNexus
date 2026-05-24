package io.github.naimjeg.damagenexus.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.*;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record AddChannelPreMultiplierOperation(
        Identifier channelId,
        Optional<Identifier> preMultiplierBucketId,
        float value
) implements DamageRuleOperation,
        ChannelReferencingOperation,
        PreMultiplierBucketReferencingOperation {

    public static final MapCodec<AddChannelPreMultiplierOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(AddChannelPreMultiplierOperation::channelId),

                    DamageRuleCodecs.PRE_MULTIPLIER_BUCKET_ID
                            .optionalFieldOf("pre_multiplier_bucket")
                            .forGetter(AddChannelPreMultiplierOperation::preMultiplierBucketId),

                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(AddChannelPreMultiplierOperation::value)
            ).apply(instance, AddChannelPreMultiplierOperation::new));

    public AddChannelPreMultiplierOperation(
            DamageChannel channel,
            Optional<Identifier> preMultiplierBucketId,
            float value
    ) {
        this(
                DamageOperationChannelIds.idOrUntyped(channel),
                preMultiplierBucketId,
                value
        );
    }

    public AddChannelPreMultiplierOperation {
        channelId = DamageOperationChannelIds.idOrUntyped(channelId);

        if (preMultiplierBucketId == null) {
            preMultiplierBucketId = Optional.empty();
        }
    }

    public DamageChannel channel() {
        return DamageOperationChannelIds.resolve(channelId);
    }

    @Override
    public Identifier type() {
        return DamageNexusOperationIds.ADD_CHANNEL_PRE_MULTIPLIER;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        int bucketId =
                DamageOperationPreMultiplierBuckets.resolveOrGeneric(
                        preMultiplierBucketId
                );

        return ctx.tryAddChannelPreMultiplier(
                DamageOperationChannelIds.resolve(channelId),
                bucketId,
                value,
                RuleTraceIds.ADD_CHANNEL_PRE_MULTIPLIER
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
    public List<Identifier> referencedChannels() {
        return List.of(channelId);
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
