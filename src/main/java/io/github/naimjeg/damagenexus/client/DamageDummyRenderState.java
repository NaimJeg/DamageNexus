package io.github.naimjeg.damagenexus.client;

import io.github.naimjeg.damagenexus.DamageNexus;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Client render state for the damage dummy.
 *
 * <p>Exposes the clean, presentation-only entity state the future Blockbench
 * model needs: whether the dummy is anchored to a pedestal, its stable
 * orientation yaw, and a future variant id. No server state is copied into
 * this class; it is derived from the entity every frame by the renderer.</p>
 *
 * <p>It deliberately extends the generic {@link LivingEntityRenderState}
 * rather than {@code HumanoidRenderState}: the dummy is a static display
 * model with no limb animation, attack animation, equipment state, or armor
 * state.</p>
 */
public class DamageDummyRenderState extends LivingEntityRenderState {

    /** Whether the dummy is currently anchored to a damage dummy pedestal. */
    private boolean anchored;

    /** Stable body orientation yaw in degrees (pedestal facing when anchored). */
    private float orientationYaw;

    /** Future Blockbench variant id (always {@code damagenexus:default}). */
    private Identifier variantId =
            Identifier.fromNamespaceAndPath(DamageNexus.MODID, "default");

    public boolean anchored() {
        return this.anchored;
    }

    public void setAnchored(boolean anchored) {
        this.anchored = anchored;
    }

    public float orientationYaw() {
        return this.orientationYaw;
    }

    public void setOrientationYaw(float orientationYaw) {
        this.orientationYaw = orientationYaw;
    }

    public Identifier variantId() {
        return this.variantId;
    }

    public void setVariantId(Identifier variantId) {
        this.variantId = variantId;
    }
}
