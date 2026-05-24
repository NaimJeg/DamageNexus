package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.rule.ChannelReferencingCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCodecs;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import net.minecraft.resources.Identifier;

import java.util.List;

public record DamageChannelIsCondition(
        Identifier channelId
) implements DamageRuleCondition, ChannelReferencingCondition {

    public static final MapCodec<DamageChannelIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRuleCodecs.DAMAGE_CHANNEL_ID
                            .fieldOf("channel")
                            .forGetter(DamageChannelIsCondition::channelId)
            ).apply(instance, DamageChannelIsCondition::new));

    public DamageChannelIsCondition(DamageChannel channel) {
        this(channel.id());
    }

    public DamageChannel channel() {
        return DamageChannelRegistry.getChannelOrUntyped(channelId);
    }

    @Override
    public Identifier type() {
        return DamageNexusConditionIds.DAMAGE_CHANNEL_IS;
    }

    @Override
    public boolean test(DamageRuleContext ctx) {
        return ctx.hasActiveDamageInChannel(channel());
    }

    @Override
    public List<Identifier> referencedChannels() {
        return List.of(channelId);
    }
}
