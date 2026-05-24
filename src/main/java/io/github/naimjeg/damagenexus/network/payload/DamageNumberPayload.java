package io.github.naimjeg.damagenexus.network.payload;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Authoritative damage presentation facts sent from the server to one player.
 */
public record DamageNumberPayload(
        long damageId,
        double x,
        double y,
        double z,
        float damage,
        boolean critical
) implements CustomPacketPayload {

    public static final Type<DamageNumberPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    DamageNexus.MODID,
                    "damage_number"
            )
    );

    public static final StreamCodec<ByteBuf, DamageNumberPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    DamageNumberPayload::damageId,
                    ByteBufCodecs.DOUBLE,
                    DamageNumberPayload::x,
                    ByteBufCodecs.DOUBLE,
                    DamageNumberPayload::y,
                    ByteBufCodecs.DOUBLE,
                    DamageNumberPayload::z,
                    ByteBufCodecs.FLOAT,
                    DamageNumberPayload::damage,
                    ByteBufCodecs.BOOL,
                    DamageNumberPayload::critical,
                    DamageNumberPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
