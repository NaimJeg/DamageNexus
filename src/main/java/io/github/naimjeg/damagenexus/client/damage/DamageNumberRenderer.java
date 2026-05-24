package io.github.naimjeg.damagenexus.client.damage;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class DamageNumberRenderer {

    private DamageNumberRenderer() {
    }

    public static void render(
            RenderLevelStageEvent.AfterTranslucentParticles event
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.font == null) {
            return;
        }

        if (ClientDamageNumberManager.active().isEmpty()) {
            return;
        }

        CameraRenderState camera =
                event.getLevelRenderState().cameraRenderState;
        PoseStack poseStack = event.getPoseStack();
        float partialTick = minecraft.getDeltaTracker()
                .getGameTimeDeltaPartialTick(false);

        poseStack.pushPose();
        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(256);
        MultiBufferSource.BufferSource bufferSource =
                MultiBufferSource.immediate(byteBufferBuilder);
        try {
            for (FloatingDamageNumber number
                    : ClientDamageNumberManager.active()) {
                poseStack.pushPose();
                poseStack.translate(
                        number.renderX() - camera.pos.x,
                        number.renderY(partialTick) - camera.pos.y,
                        number.renderZ() - camera.pos.z
                );
                poseStack.mulPose(camera.orientation);
                float worldScale = number.worldScale(partialTick);
                poseStack.scale(
                        worldScale,
                        -worldScale,
                        worldScale
                );

                String text = DamageNumberFormatter.format(number.damage());
                float x = -minecraft.font.width(text) / 2.0F;
                int alpha = Math.round(
                        number.alpha(partialTick) * 255.0F
                );
                int color = (alpha << 24)
                        | (number.critical() ? 0xFFD34E : 0xFFFFFF);

                minecraft.font.drawInBatch(
                        text,
                        x,
                        0.0F,
                        color,
                        true,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        LightCoordsUtil.FULL_BRIGHT
                );
                poseStack.popPose();
            }
            bufferSource.endBatch();
        } finally {
            byteBufferBuilder.close();
        }
        poseStack.popPose();
    }
}
