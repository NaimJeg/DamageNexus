package io.github.naimjeg.damagenexus.network.payload;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNumberPayloadTest {

    @Test
    void streamCodecRoundTripsAuthoritativeFacts() {
        DamageNumberPayload original = new DamageNumberPayload(
                987654321L,
                12.5D,
                64.25D,
                -7.75D,
                31.5F,
                true
        );
        ByteBuf buffer = Unpooled.buffer();
        try {
            DamageNumberPayload.STREAM_CODEC.encode(buffer, original);
            DamageNumberPayload decoded =
                    DamageNumberPayload.STREAM_CODEC.decode(buffer);

            assertEquals(original.damageId(), decoded.damageId());
            assertEquals(original.x(), decoded.x());
            assertEquals(original.y(), decoded.y());
            assertEquals(original.z(), decoded.z());
            assertEquals(original.damage(), decoded.damage());
            assertTrue(decoded.critical());
        } finally {
            buffer.release();
        }
    }
}
