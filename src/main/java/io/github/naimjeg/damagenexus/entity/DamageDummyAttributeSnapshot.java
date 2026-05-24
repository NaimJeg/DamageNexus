package io.github.naimjeg.damagenexus.entity;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Authoritative server snapshot displayed by one pedestal menu. */
public record DamageDummyAttributeSnapshot(
        BlockPos anchorPos,
        boolean available,
        List<DamageDummyAttributeView> attributes
) {

    private static final StreamCodec<ByteBuf, List<DamageDummyAttributeView>>
            ATTRIBUTE_LIST_CODEC = DamageDummyAttributeView.STREAM_CODEC.apply(
            ByteBufCodecs.list(DamageDummyAttributeProtocol.MAX_ATTRIBUTES)
    );

    public static final StreamCodec<ByteBuf, DamageDummyAttributeSnapshot>
            STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DamageDummyAttributeSnapshot::anchorPos,
            ByteBufCodecs.BOOL,
            DamageDummyAttributeSnapshot::available,
            ATTRIBUTE_LIST_CODEC,
            DamageDummyAttributeSnapshot::attributes,
            DamageDummyAttributeSnapshot::new
    );

    public DamageDummyAttributeSnapshot {
        anchorPos = Objects.requireNonNull(anchorPos, "anchorPos").immutable();
        attributes = List.copyOf(Objects.requireNonNull(
                attributes,
                "attributes"
        ));
        if (attributes.size() > DamageDummyAttributeProtocol.MAX_ATTRIBUTES) {
            throw new IllegalArgumentException("too many damage-dummy attributes");
        }
        if (!available && !attributes.isEmpty()) {
            throw new IllegalArgumentException(
                    "unavailable snapshot cannot contain attributes"
            );
        }
    }

    public static DamageDummyAttributeSnapshot unavailable(BlockPos anchorPos) {
        return new DamageDummyAttributeSnapshot(anchorPos, false, List.of());
    }
}
