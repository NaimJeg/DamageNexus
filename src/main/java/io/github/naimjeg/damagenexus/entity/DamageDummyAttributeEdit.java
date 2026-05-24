package io.github.naimjeg.damagenexus.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** One requested base-value mutation in a server-validated batch. */
public record DamageDummyAttributeEdit(
        Identifier attributeId,
        double requestedBaseValue
) {

    public static final StreamCodec<ByteBuf, DamageDummyAttributeEdit>
            STREAM_CODEC = StreamCodec.composite(
            DamageDummyAttributeProtocol.IDENTIFIER_CODEC,
            DamageDummyAttributeEdit::attributeId,
            ByteBufCodecs.DOUBLE,
            DamageDummyAttributeEdit::requestedBaseValue,
            DamageDummyAttributeEdit::new
    );
}
