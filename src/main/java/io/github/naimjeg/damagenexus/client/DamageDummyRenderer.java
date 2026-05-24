package io.github.naimjeg.damagenexus.client;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.entity.DamageDummyEntity;
import io.github.naimjeg.damagenexus.registry.ModEntityTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Static single-model display renderer for the damage dummy.
 *
 * <p>Architecture: {@code DamageDummyEntity -> DamageDummyRenderer ->
 * DamageDummyModel -> static mesh}. The renderer copies only the entity's
 * presentation state (anchored, body yaw, variant id) into
 * {@link DamageDummyRenderState} and the generic
 * {@link LivingEntityRenderer} submits the static model. There are no armor,
 * equipment, hand-pose, or animation layers.</p>
 *
 * <p>The dummy has its own namespace: the renderer always binds
 * {@link DamageDummyModel} to
 * {@code damagenexus:textures/entity/damage_dummy.png}, never to any
 * Minecraft player texture. The model comes from the standard layer
 * lifecycle: {@code DamageDummyModel.LAYER_LOCATION} is registered on the
 * client mod event bus and baked here through
 * {@code context.bakeLayer(...)}. Future Blockbench variants will map
 * {@link DamageDummyRenderState#variantId()} to their own model classes
 * here; the current single {@link DamageDummyModel} is the default.</p>
 *
 * <p>Only referenced from the client-only mod entry point
 * ({@code ModClientHandler}), so this class is never loaded on a dedicated
 * server.</p>
 */
public class DamageDummyRenderer
        extends LivingEntityRenderer<
        DamageDummyEntity,
        DamageDummyRenderState,
        DamageDummyModel
        > {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    DamageNexus.MODID,
                    "textures/entity/damage_dummy.png"
            );

    public DamageDummyRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new DamageDummyModel(
                        context.bakeLayer(DamageDummyModel.LAYER_LOCATION)
                ),
                0.5F
        );
    }

    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                ModEntityTypes.DAMAGE_DUMMY.get(),
                DamageDummyRenderer::new
        );
    }

    @Override
    public DamageDummyRenderState createRenderState() {
        return new DamageDummyRenderState();
    }

    @Override
    public void extractRenderState(
            DamageDummyEntity entity,
            DamageDummyRenderState state,
            float partialTicks
    ) {
        super.extractRenderState(entity, state, partialTicks);
        state.setAnchored(entity.isAnchored());
        state.setOrientationYaw(entity.getYRot());
        state.setVariantId(entity.getVariantId());
    }

    @Override
    protected boolean shouldShowName(
            DamageDummyEntity entity,
            double distanceToCameraSq
    ) {
        return false;
    }

    @Override
    public Identifier getTextureLocation(DamageDummyRenderState state) {
        return TEXTURE;
    }
}
