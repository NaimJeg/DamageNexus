package io.github.naimjeg.damagenexus.network.payload;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeSnapshot;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative attribute snapshot for one currently open menu. */
public record DamageDummyAttributesPayload(
        int containerId,
        DamageDummyAttributeSnapshot snapshot
) implements CustomPacketPayload {

    public static final Type<DamageDummyAttributesPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    DamageNexus.MODID,
                    "damage_dummy_attributes"
            )
    );

    public static final StreamCodec<ByteBuf, DamageDummyAttributesPayload>
            STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DamageDummyAttributesPayload::containerId,
            DamageDummyAttributeSnapshot.STREAM_CODEC,
            DamageDummyAttributesPayload::snapshot,
            DamageDummyAttributesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
