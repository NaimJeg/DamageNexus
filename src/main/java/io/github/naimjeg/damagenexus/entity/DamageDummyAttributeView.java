package io.github.naimjeg.damagenexus.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Immutable, runtime-reference-free presentation data for one real attribute. */
public record DamageDummyAttributeView(
        Identifier id,
        String translationKey,
        double baseValue,
        double effectiveValue,
        double defaultValue
) {

    public static final StreamCodec<ByteBuf, DamageDummyAttributeView>
            STREAM_CODEC = StreamCodec.composite(
            DamageDummyAttributeProtocol.IDENTIFIER_CODEC,
            DamageDummyAttributeView::id,
            DamageDummyAttributeProtocol.TRANSLATION_KEY_CODEC,
            DamageDummyAttributeView::translationKey,
            ByteBufCodecs.DOUBLE,
            DamageDummyAttributeView::baseValue,
            ByteBufCodecs.DOUBLE,
            DamageDummyAttributeView::effectiveValue,
            ByteBufCodecs.DOUBLE,
            DamageDummyAttributeView::defaultValue,
            DamageDummyAttributeView::new
    );
}
