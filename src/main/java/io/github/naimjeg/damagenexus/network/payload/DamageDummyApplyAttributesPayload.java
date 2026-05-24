package io.github.naimjeg.damagenexus.network.payload;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeEdit;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeProtocol;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to atomically edit changed base values on one open menu. */
public record DamageDummyApplyAttributesPayload(
        int containerId,
        BlockPos anchorPos,
        List<DamageDummyAttributeEdit> edits
) implements CustomPacketPayload {

    private static final StreamCodec<ByteBuf, List<DamageDummyAttributeEdit>>
            EDIT_LIST_CODEC = DamageDummyAttributeEdit.STREAM_CODEC.apply(
            ByteBufCodecs.list(DamageDummyAttributeProtocol.MAX_ATTRIBUTES)
    );

    public static final Type<DamageDummyApplyAttributesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    DamageNexus.MODID,
                    "damage_dummy_apply_attributes"
            ));

    public static final StreamCodec<
            ByteBuf,
            DamageDummyApplyAttributesPayload
            > STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DamageDummyApplyAttributesPayload::containerId,
            BlockPos.STREAM_CODEC,
            DamageDummyApplyAttributesPayload::anchorPos,
            EDIT_LIST_CODEC,
            DamageDummyApplyAttributesPayload::edits,
            DamageDummyApplyAttributesPayload::new
    );

    public DamageDummyApplyAttributesPayload {
        anchorPos = anchorPos == null ? null : anchorPos.immutable();
        edits = edits == null ? null : List.copyOf(edits);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
