package io.github.naimjeg.damagenexus.api.client.phrase;

import net.minecraft.resources.Identifier;
import java.util.Objects;

public record ChannelValue(Identifier channelId) implements PhraseValue {
    public ChannelValue {
        Objects.requireNonNull(channelId, "channelId");
    }
}
