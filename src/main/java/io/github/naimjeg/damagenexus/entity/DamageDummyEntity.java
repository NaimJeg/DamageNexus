package io.github.naimjeg.damagenexus.entity;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import io.github.naimjeg.damagenexus.block.DamageDummyBlock;
import io.github.naimjeg.damagenexus.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A {@link LivingEntity} combat target with a static display renderer.
 *
 * <p>The dummy is a real {@link LivingEntity}: ordinary attacks enter through
 * the normal Minecraft damage path, are processed by the vanilla/NeoForge/
 * DamageNexus pipeline, and change this entity's health like any other
 * LivingEntity. It never calculates or replaces DamageNexus damage itself.
 * The entity carries no rendering logic; the client renderer decides how the
 * static display model is drawn.</p>
 *
 * <p>Architectural invariant: this entity pre-attaches registered entity
 * attributes through the supported NeoForge attribute lifecycle
 * ({@code ModEntityAttributes}) so tooling such as the attribute GUI can
 * enumerate and edit its real AttributeInstances without maintaining a
 * hardcoded attribute list or dynamically mutating AttributeSupplier
 * internals.</p>
 *
 * <p>Static display target: the dummy has no AI, no goal selector, no
 * navigation, and no movement. Gravity is disabled and {@link #travel} is a
 * no-op, so neither gravity nor knockback nor fluid displacement can move it.
 * It is a display dummy, not a simulated creature.</p>
 *
 * <p>Anchored mode: when the dummy is owned by a {@link DamageDummyBlock}
 * pedestal it stores the anchor {@link BlockPos} and is positioned on top of
 * the low-profile plate (feet at {@code anchor + (0.5, BASE_HEIGHT, 0.5)},
 * see {@link #getAnchoredPosition}). Anchored dummies are repositioned every
 * server tick, take normal damage through the real DamageNexus pipeline, and
 * are effectively immortal: the terminal death path is guarded without
 * cancelling the damage, and health returns to {@code getMaxHealth()} on the
 * following server tick once the damage transaction has finished. A manually
 * summoned ({@code /summon}) dummy has no anchor and keeps the original
 * standalone behavior, including normal death.</p>
 *
 * <p>Ownership model: {@link DamageDummyBlockEntity} is the primary lifecycle
 * controller for anchored dummies. An anchored {@code DamageDummyEntity}
 * additionally performs fail-closed self-validation: if its anchor block is
 * absent while the entity is ticking, it discards itself. An anchored dummy
 * never transitions into a standalone dummy because its pedestal disappears;
 * the pedestal either exists (owned anchored entity) or does not (owned
 * entity discarded). Standalone dummies never participate in pedestal
 * ownership.</p>
 */
public class DamageDummyEntity extends LivingEntity {

    private static final String TAG_ANCHOR = "DamageNexusAnchor";

    @Nullable
    private BlockPos anchorPos;

    /**
     * Set when anchored lethal damage reaches the terminal death path. The
     * health restore is intentionally deferred to the next server tick so the
     * in-flight damage transaction (settlement) observes the reduced/lethal
     * result before health returns to maximum.
     */
    private boolean pendingAnchorRestore;

    public DamageDummyEntity(EntityType<? extends DamageDummyEntity> type, Level level) {
        super(type, level);
        // Static display target: never affected by gravity. Movement is
        // additionally impossible because travel() is a no-op below.
        this.setNoGravity(true);
    }

    /**
     * Baseline supplier. Every registered entity Attribute that is not already
     * present is added generically in {@code EntityAttributeModificationEvent}
     * (see {@code ModEntityAttributes}), so this baseline only needs the
     * ordinary LivingEntity attributes required for a functioning entity.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    /**
     * The only abstract LivingEntity method: the dummy is right-handed by
     * default. This is pure renderer-facing state; the dummy never holds or
     * uses items.
     */
    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    /**
     * Movement is disabled entirely: no gravity, no input movement, no
     * knockback displacement, no fluid pushing. The dummy is a static display
     * target; {@code setDeltaMovement} may be written by the damage/knockback
     * pipeline but can never translate into a position change.
     */
    @Override
    public void travel(Vec3 input) {
        // Intentionally empty: the dummy never moves.
    }

    /** Whether this dummy is owned by a damage dummy pedestal. */
    public boolean isAnchored() {
        return this.anchorPos != null;
    }

    /** The pedestal position this dummy is anchored to, if any. */
    public Optional<BlockPos> anchorPos() {
        return Optional.ofNullable(this.anchorPos);
    }

    /** Whether this dummy is anchored to exactly {@code pos}. */
    public boolean isAnchoredAt(BlockPos pos) {
        return this.anchorPos != null && this.anchorPos.equals(pos);
    }

    /**
     * Binds this dummy to the pedestal at {@code pos}: stores the anchor,
     * disables gravity, snaps to the exact pedestal-top position and faces
     * {@code yaw}. Idempotent.
     */
    public void bindToAnchor(BlockPos pos, float yaw) {
        this.anchorPos = pos.immutable();
        this.setNoGravity(true);
        Vec3 feet = getAnchoredPosition(this.anchorPos);
        this.setPos(feet.x(), feet.y(), feet.z());
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    /**
     * The single authoritative {@link Entity#setPos} position for a dummy
     * anchored to the pedestal at {@code pos}: the plate occupies Y
     * 0..{@link DamageDummyBlock#BASE_HEIGHT}, so the entity stands at
     * {@code pos + (0.5, BASE_HEIGHT, 0.5)}, directly on the plate. All
     * placement, clearance, binding and server-correction call sites must use
     * this method instead of duplicating the coordinate formula.
     */
    public static Vec3 getAnchoredPosition(BlockPos pos) {
        return new Vec3(
                pos.getX() + 0.5D,
                pos.getY() + DamageDummyBlock.BASE_HEIGHT,
                pos.getZ() + 0.5D
        );
    }

    /**
     * Stable display variant id for future Blockbench model variants. The
     * single default variant is {@code damagenexus:default}; the client
     * renderer uses this id to select the model/texture.
     */
    public Identifier getVariantId() {
        return DamageNexusIds.id("default");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.isAnchored()) {
            this.serverAnchoredTick();
        }
    }

    /**
     * Structural invariant enforcement for anchored dummies. The block entity
     * is the primary lifecycle controller and normally discards the entity
     * when the pedestal is removed, but this entity-side check is the
     * fail-closed second safety layer: if the anchor block is absent while
     * this entity is ticking, the entity discards itself. The anchor is kept
     * until removal so ownership/debug state stays unambiguous; an anchored
     * dummy never becomes standalone because its pedestal disappeared.
     * Otherwise gravity stays suppressed, any damage/knockback momentum is
     * zeroed, the entity snaps back to the exact anchor position, follows the
     * pedestal's facing, and full health is restored once the damage
     * transaction has observed the hit.
     */
    private void serverAnchoredTick() {
        if (this.anchorPos == null) {
            return;
        }
        if (!this.level().getBlockState(this.anchorPos)
                .is(ModBlocks.DAMAGE_DUMMY.get())) {
            this.discard();
            return;
        }

        this.setNoGravity(true);
        Vec3 feet = getAnchoredPosition(this.anchorPos);
        if (this.getX() != feet.x()
                || this.getY() != feet.y()
                || this.getZ() != feet.z()) {
            this.setPos(feet.x(), feet.y(), feet.z());
        }
        if (!this.getDeltaMovement().equals(Vec3.ZERO)) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        BlockState state = this.level().getBlockState(this.anchorPos);
        float yaw = state.getValue(DamageDummyBlock.FACING).toYRot();
        if (Math.abs(this.getYRot() - yaw) > 1.0E-4F) {
            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.setYBodyRot(yaw);
        }

        if (this.pendingAnchorRestore) {
            this.setHealth(this.getMaxHealth());
            this.pendingAnchorRestore = false;
        }
        if (this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    /**
     * Terminal death guard. Standalone dummies die exactly as before. For
     * anchored dummies the already-applied attack is never rejected and
     * {@code super.die} is deliberately not called, which in the 26.1.2
     * LivingEntity death path means:
     *
     * <ul>
     *   <li>{@code dead} flag stays {@code false} (only {@code super.die}
     *       sets it);</li>
     *   <li>{@code deathTime} is left untouched and the death-tick removal
     *       sequence never starts;</li>
     *   <li>no {@code ENTITY_DIE} game event, death-sound broadcast, or
     *       {@code DYING} pose is emitted;</li>
     *   <li>the entity is never removed.</li>
     * </ul>
     *
     * <p>Restoring a positive health value keeps {@code isDeadOrDying()}
     * false so {@code tickDeath()} cannot run, and the next anchored server
     * tick restores full health. DamageNexus still sees the damage accepted,
     * health reduced, and the lethal result.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (this.isAnchored()) {
            this.setHealth(1.0F);
            this.pendingAnchorRestore = true;
            return;
        }
        super.die(source);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.anchorPos != null) {
            output.store(TAG_ANCHOR, BlockPos.CODEC, this.anchorPos);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.anchorPos = input.read(TAG_ANCHOR, BlockPos.CODEC).orElse(null);
    }
}
