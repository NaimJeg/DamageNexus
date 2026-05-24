package io.github.naimjeg.damagenexus.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Shared limits and bounded codecs for the damage-dummy attribute protocol. */
public final class DamageDummyAttributeProtocol {

    /** Well above the normal registry size, but small enough to bound packets. */
    public static final int MAX_ATTRIBUTES = 512;

    /** Registry identifiers and translation keys are never allowed to be huge. */
    public static final int MAX_TEXT_LENGTH = 256;

    public static final StreamCodec<ByteBuf, Identifier> IDENTIFIER_CODEC =
            ByteBufCodecs.stringUtf8(MAX_TEXT_LENGTH).map(
                    Identifier::parse,
                    Identifier::toString
            );

    public static final StreamCodec<ByteBuf, String> TRANSLATION_KEY_CODEC =
            ByteBufCodecs.stringUtf8(MAX_TEXT_LENGTH);

    private DamageDummyAttributeProtocol() {
    }
}
