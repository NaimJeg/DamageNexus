package io.github.naimjeg.damagenexus.builtin.rule.operation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageMutationResult;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.ChannelReferencingOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCodecs;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.RuleTraceIds;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import net.minecraft.resources.Identifier;

import java.util.List;

public record GainExtraDamageOperation(
        Identifier basedOnChannel,
        Identifier toChannel,
        float ratio
) implements DamageRuleOperation, ChannelReferencingOperation {

    public static final MapCodec<GainExtraDamageOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("based_on_channel")
                            .forGetter(GainExtraDamageOperation::basedOnChannel),

                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("to_channel")
                            .forGetter(GainExtraDamageOperation::toChannel),

                    DamageRuleCodecs.NON_NEGATIVE_FLOAT
                            .fieldOf("ratio")
                            .forGetter(GainExtraDamageOperation::ratio)
            ).apply(instance, GainExtraDamageOperation::new));

    @Override
    public Identifier type() {
        return DamageNexusOperationIds.GAIN_EXTRA_DAMAGE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryGainExtraDamage(
                DamageChannelRegistry.getChannelOrUntyped(basedOnChannel),
                DamageChannelRegistry.getChannelOrUntyped(toChannel),
                ratio,
                RuleTraceIds.GAIN_EXTRA_DAMAGE
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(DamagePhase.TYPE_SCALING);
    }

    @Override
    public List<Identifier> referencedChannels() {
        return List.of(basedOnChannel, toChannel);
    }

    @Override
    public float stackingValue() {
        return ratio;
    }
}
