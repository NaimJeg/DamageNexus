package io.github.naimjeg.damagenexus.network.payload;

import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeEdit;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeSnapshot;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeView;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageDummyAttributePayloadTest {

    private static final Identifier ATTRIBUTE_ID =
            Identifier.withDefaultNamespace("max_health");

    @Test
    void applyPayloadCodecRoundTripsEveryField() {
        DamageDummyApplyAttributesPayload original =
                new DamageDummyApplyAttributesPayload(
                        42,
                        new BlockPos(7, 80, -3),
                        List.of(
                                new DamageDummyAttributeEdit(
                                        ATTRIBUTE_ID,
                                        40.5D
                                )
                        )
                );
        assertEquals(original, roundTripApply(original));
    }

    @Test
    void authoritativeSnapshotPayloadCodecRoundTripsEveryField() {
        DamageDummyAttributeSnapshot snapshot =
                new DamageDummyAttributeSnapshot(
                        new BlockPos(7, 80, -3),
                        true,
                        List.of(new DamageDummyAttributeView(
                                ATTRIBUTE_ID,
                                "attribute.name.generic.max_health",
                                40.0D,
                                45.0D,
                                20.0D
                        ))
                );
        DamageDummyAttributesPayload original =
                new DamageDummyAttributesPayload(42, snapshot);
        ByteBuf buffer = Unpooled.buffer();
        try {
            DamageDummyAttributesPayload.STREAM_CODEC.encode(buffer, original);
            assertEquals(
                    original,
                    DamageDummyAttributesPayload.STREAM_CODEC.decode(buffer)
            );
        } finally {
            buffer.release();
        }
    }

    @Test
    void applyPayloadCodecRejectsAnOversizedList() {
        List<DamageDummyAttributeEdit> edits = new ArrayList<>();
        for (int index = 0; index < 513; index++) {
            edits.add(new DamageDummyAttributeEdit(ATTRIBUTE_ID, index));
        }
        DamageDummyApplyAttributesPayload payload =
                new DamageDummyApplyAttributesPayload(
                        42,
                        BlockPos.ZERO,
                        edits
                );
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(
                    io.netty.handler.codec.EncoderException.class,
                    () -> DamageDummyApplyAttributesPayload.STREAM_CODEC
                            .encode(buffer, payload)
            );
        } finally {
            buffer.release();
        }
    }

    private static DamageDummyApplyAttributesPayload roundTripApply(
            DamageDummyApplyAttributesPayload original
    ) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            DamageDummyApplyAttributesPayload.STREAM_CODEC.encode(
                    buffer,
                    original
            );
            return DamageDummyApplyAttributesPayload.STREAM_CODEC.decode(
                    buffer
            );
        } finally {
            buffer.release();
        }
    }
}
