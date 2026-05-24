package io.github.naimjeg.damagenexus.builtin.rule.operation;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import net.minecraft.resources.Identifier;

final class DamageOperationChannelIds {

    private DamageOperationChannelIds() {
    }

    static Identifier idOrUntyped(DamageChannel channel) {
        return channel == null
                ? DamageChannel.UNTYPED_ID
                : channel.id();
    }

    static Identifier idOrUntyped(Identifier channelId) {
        return channelId == null
                ? DamageChannel.UNTYPED_ID
                : channelId;
    }

    static DamageChannel resolve(Identifier channelId) {
        return DamageChannelRegistry.getChannelOrUntyped(
                idOrUntyped(channelId)
        );
    }
}