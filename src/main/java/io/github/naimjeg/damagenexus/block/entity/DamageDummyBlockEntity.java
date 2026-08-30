package io.github.naimjeg.damagenexus.block.entity;

import io.github.naimjeg.damagenexus.block.DamageDummyBlock;
import io.github.naimjeg.damagenexus.entity.DamageDummyEntity;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeSnapshot;
import io.github.naimjeg.damagenexus.menu.DamageDummyMenu;
import io.github.naimjeg.damagenexus.registry.ModBlocks;
import io.github.naimjeg.damagenexus.registry.ModBlockEntityTypes;
import io.github.naimjeg.damagenexus.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lifecycle controller for the anchored {@link DamageDummyEntity}.
 *
 * <p>This block entity is deliberately NOT an attribute store. It only
 * persists the linked entity's {@link UUID} (never the transient numeric
 * entity id), guarantees exactly one anchored dummy per pedestal, establishes
 * fresh/adopted ownership, reconciles duplicates and terminates a previously
 * bound pair whose entity is permanently missing,
 * and discards every owned entity when the physical block is actually
 * removed. It is the primary lifecycle controller: anchored dummies are
 * synthetic state belonging to the physical pedestal block. An anchored
 * {@code DamageDummyEntity} additionally performs fail-closed
 * self-validation: if its anchor block is absent while the entity is ticking,
 * it discards itself. Standalone dummies never participate in pedestal
 * ownership. All combat/attribute authority stays on the entity.</p>
 */
public class DamageDummyBlockEntity extends BlockEntity implements MenuProvider {

    private static final String KEY_LINKED_UUID = "LinkedDummy";
    private static final Component CONTAINER_TITLE =
            Component.translatable("container.damagenexus.damage_dummy");

    /** How often the ownership reconciliation runs (server ticks). */
    private static final int RECONCILE_INTERVAL = 20;

    /**
     * Stateful guard against the chunk-load race ONLY. A freshly deserialized
     * block entity begins with this flag {@code false}; the first
     * reconciliation after a chunk reload observes a missing entity and only
     * records completion. A later reconciliation may spawn for a still-UNBOUND
     * pedestal, but a persisted bound UUID that remains missing terminates the
     * pedestal instead. That deferral prevents treating a linked entity that
     * merely loads a moment later as terminally missing.
     *
     * <p>Fresh BlockItem placement never enters this grace phase: it is
     * initialized synchronously through
     * {@link #initializeFreshPlacement(ServerLevel)} inside the placement
     * callback, which completes this flag immediately. If that immediate
     * spawn attempt fails, the flag is still completed so the very next
     * reconciliation cycle retries without an artificial reload delay.
     * Runtime-only state; never serialized.</p>
     */
    private boolean initialSyncCompleted;

    private long tickCount;

    @Nullable
    private UUID linkedDummyUuid;

    public DamageDummyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DAMAGE_DUMMY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return CONTAINER_TITLE;
    }

    /**
     * Server-side menu factory: the slot-free management menu receives this
     * block entity so it can capture an initial authoritative snapshot and
     * validate the menu against the physical block.
     */
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new DamageDummyMenu(containerId, inventory, this);
    }

    /**
     * Sends the anchor {@link BlockPos}, followed by the snapshot already
     * captured by the server menu. The block position is the authoritative
     * persistent identity; the linked entity's transient numeric id is never
     * used.
     */
    @Override
    public void writeClientSideData(
            AbstractContainerMenu menu,
            RegistryFriendlyByteBuf buffer
    ) {
        buffer.writeBlockPos(this.worldPosition);
        DamageDummyAttributeSnapshot.STREAM_CODEC.encode(
                buffer,
                menu instanceof DamageDummyMenu dummyMenu
                        ? dummyMenu.snapshot()
                        : DamageDummyAttributeSnapshot.unavailable(
                                this.worldPosition
                        )
        );
    }

    /**
     * Safely resolves only the persisted UUID-owned dummy. This query never
     * adopts a nearby entity and never changes reconciliation state.
     */
    public Optional<DamageDummyEntity> resolveManagedDummy(ServerLevel level) {
        if (this.linkedDummyUuid == null
                || this.level != level
                || level.getBlockEntity(this.worldPosition) != this
                || !level.getBlockState(this.worldPosition)
                .is(ModBlocks.DAMAGE_DUMMY.get())) {
            return Optional.empty();
        }
        Entity resolved = level.getEntity(this.linkedDummyUuid);
        if (!(resolved instanceof DamageDummyEntity dummy)
                || dummy.isRemoved()
                || !dummy.isAnchoredAt(this.worldPosition)) {
            return Optional.empty();
        }
        return Optional.of(dummy);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            DamageDummyBlockEntity blockEntity
    ) {
        blockEntity.tickCount++;
        if (blockEntity.tickCount % RECONCILE_INTERVAL == 0) {
            blockEntity.reconcile((ServerLevel) level, pos);
        }
    }

    /** Current linked dummy UUID, or null when no relationship is stored. */
    @Nullable
    public UUID linkedDummyUuid() {
        return this.linkedDummyUuid;
    }

    /**
     * Test support: creates a fresh instance that simulates a chunk reload
     * where the linked dummy {@link UUID} is already known but no
     * reconciliation has run yet ({@code initialSyncCompleted} is
     * {@code false}). The returned block entity is intentionally not placed
     * in a level; the reload-race GameTest drives its reconciliation directly
     * through {@link #reconcileNow(ServerLevel)}.
     */
    public static DamageDummyBlockEntity createUnreconciled(
            BlockPos pos,
            BlockState state,
            @Nullable UUID linkedUuid
    ) {
        DamageDummyBlockEntity blockEntity = new DamageDummyBlockEntity(pos, state);
        blockEntity.linkedDummyUuid = linkedUuid;
        return blockEntity;
    }

    /**
     * Test support: runs exactly one ownership reconciliation immediately.
     * This lets the reload-race GameTest observe the first (deferring) and a
     * later (terminating when bound, spawning when unbound) reconciliation
     * deterministically instead of depending
     * on the 20-tick ticker cadence.
     */
    public void reconcileNow(ServerLevel level) {
        this.reconcile(level, this.worldPosition);
    }

    /**
     * Fresh normal BlockItem placement: establishes the initial block ->
     * entity relation synchronously, immediately after the pedestal state is
     * in the world. Called only from
     * {@link DamageDummyBlock#setPlacedBy} on the server, never from the
     * periodic ticker and never on the client.
     *
     * <p>This is deliberately NOT the chunk-reload path. A reload keeps the
     * first-reconciliation deferral (see {@link #reconcile}) because a
     * delayed but valid linked entity may still load; fresh placement has no
     * such ambiguity, so the ownership link is established during the
     * placement action itself.</p>
     *
     * <p>Idempotent by construction: a valid linked keeper is kept. A fresh
     * unbound pedestal adopts one deterministic local anchored dummy, or
     * spawns exactly one when none exists. A previously bound but invalid
     * identity terminates instead of being replaced.</p>
     *
     * <p>If the immediate spawn fails (clearance became invalid, entity
     * creation returned null, or {@code addFreshEntity} failed), the block is
     * left untouched and nothing is thrown. Because this path is known NOT to
     * be a chunk reload, {@code initialSyncCompleted} is completed even on
     * failure: ownership is not falsely claimed (the linked UUID is only set
     * on a real successful spawn/adoption), but the next normal
     * reconciliation cycle may retry immediately instead of adding an extra
     * artificial reload grace period.</p>
     */
    public void initializeFreshPlacement(ServerLevel level) {
        // Fresh placement is not a chunk reload; never defer to a second
        // reconcile cycle just to establish ownership.
        this.initialSyncCompleted = true;

        BlockPos pos = this.worldPosition;
        if (!level.getBlockState(pos).is(ModBlocks.DAMAGE_DUMMY.get())) {
            // Idempotency check 1: a stale invocation after the pedestal was
            // already removed/replaced must not spawn an orphaned dummy.
            return;
        }

        boolean wasBound = this.linkedDummyUuid != null;

        // Idempotency check 2: a valid linked keeper is kept as-is.
        DamageDummyEntity keeper = this.resolveLinkedKeeper(level, pos);
        List<DamageDummyEntity> anchored = this.findAnchoredDummies(level, pos);

        if (wasBound && keeper == null) {
            this.destroyBrokenPair(level, pos);
            return;
        }

        // Idempotency check 3: adopt exactly one anchored dummy if one (or
        // more) already exists, discarding duplicates deterministically.
        if (keeper == null && !anchored.isEmpty()) {
            keeper = selectKeeper(anchored);
        }

        if (keeper != null) {
            discardDuplicatesExcept(anchored, keeper);
            this.linkedDummyUuid = keeper.getUUID();
            this.positionAndOrient(keeper, level, pos);
            this.initialSyncCompleted = true;
            this.setChanged();
            return;
        }

        // Idempotency check 4: only now, with no valid anchored entity
        // anywhere, spawn exactly one new dummy through the shared path.
        this.spawnDummy(level, pos);

        // Whether or not the spawn succeeded, this BlockEntity is no longer
        // in a reload ambiguity state: its ownership has been established
        // synchronously (or a failed attempt may be retried by the next
        // normal reconciliation immediately).
        this.initialSyncCompleted = true;
        if (this.linkedDummyUuid != null) {
            this.setChanged();
        }
    }

    /**
     * Block actually removed/replaced: discard the anchored entity. A pure
     * {@code FACING} rewrite of the same block never reaches this callback,
     * so the entity survives orientation-only changes.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level instanceof ServerLevel serverLevel) {
            discardOwnedDummies(serverLevel, this.worldPosition);
        }
    }

    /**
     * The single authoritative cleanup for every entity owned by this
     * pedestal. Runs on pedestal destruction/error paths only and is limited
     * to the small anchor AABB, so its cost is negligible.
     *
     * <p>Two complementary paths are required. The fast path resolves the
     * persisted linked {@link UUID}, but that UUID may be stale, null, not
     * yet adopted, or point at an already-removed entity, and there may be
     * duplicate anchored dummies or repaired entities whose UUID has not been
     * persisted yet. The local safety scan therefore removes every remaining
     * {@link DamageDummyEntity} anchored to this pedestal regardless of the
     * stored link. Duplicate/removal is idempotent: entities already removed
     * by the UUID path are filtered out of the scan by
     * {@code !dummy.isRemoved()}.</p>
     */
    private void discardOwnedDummies(ServerLevel level, BlockPos anchorPos) {
        UUID linked = this.linkedDummyUuid;
        if (linked != null) {
            Entity resolved = level.getEntity(linked);
            if (resolved instanceof DamageDummyEntity dummy
                    && dummy.isAnchoredAt(anchorPos)
                    && !dummy.isRemoved()) {
                dummy.discardWithoutAnchorDestruction();
            }
        }

        List<DamageDummyEntity> local = level.getEntitiesOfClass(
                DamageDummyEntity.class,
                searchAabb(anchorPos),
                dummy -> dummy.isAnchoredAt(anchorPos) && !dummy.isRemoved()
        );
        for (DamageDummyEntity dummy : local) {
            dummy.discardWithoutAnchorDestruction();
        }

        this.linkedDummyUuid = null;
        this.setChanged();
    }

    /**
     * Local, deterministic ownership reconciliation:
     *
     * <ol>
     *   <li>confirm this block entity still belongs to {@code DamageDummyBlock};</li>
     *   <li>resolve the stored UUID and keep that entity when valid;</li>
     *   <li>if a persisted UUID remains unresolved after reload grace,
     *       destroy the pedestal without adopting or spawning a replacement;</li>
     *   <li>only for an unbound pedestal, search a small AABB and adopt the
     *       single found anchored dummy;</li>
     *   <li>when several exist, keep one deterministically and discard the rest;</li>
     *   <li>when none exists, the first reconciliation after a chunk reload
     *       only records that the initial sync is complete; second and later
     *       reconciliations spawn an initial entity for an unbound pedestal
     *       (only if there is enough free space). Fresh BlockItem placement
     *       bypasses this deferral through
     *       {@link #initializeFreshPlacement}.</li>
     * </ol>
     */
    private void reconcile(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.DAMAGE_DUMMY.get())) {
            // Second fail-closed cleanup path: a stale BlockEntity executing
            // after its physical block disappeared must not merely forget the
            // UUID; it must remove every owned entity.
            this.discardOwnedDummies(level, pos);
            return;
        }

        DamageDummyEntity keeper = this.resolveLinkedKeeper(level, pos);
        List<DamageDummyEntity> anchored = this.findAnchoredDummies(level, pos);

        if (keeper != null) {
            discardDuplicatesExcept(anchored, keeper);
            this.positionAndOrient(keeper, level, pos);
            this.initialSyncCompleted = true;
            return;
        }

        if (this.linkedDummyUuid != null) {
            // A persisted UUID is the durable evidence that this pedestal was
            // already bound. Do not clear it on a temporarily unresolved
            // lookup: the first reconciliation is reload grace, and a later
            // miss terminates the pair instead of resurrecting a new UUID or
            // adopting a nearby duplicate.
            if (this.initialSyncCompleted) {
                this.destroyBrokenPair(level, pos);
            } else {
                this.initialSyncCompleted = true;
            }
            return;
        }

        if (anchored.isEmpty()) {
            if (this.initialSyncCompleted) {
                this.spawnDummy(level, pos);
            } else {
                this.initialSyncCompleted = true;
            }
            return;
        }

        keeper = selectKeeper(anchored);
        discardDuplicatesExcept(anchored, keeper);
        this.linkedDummyUuid = keeper.getUUID();
        this.positionAndOrient(keeper, level, pos);
        this.initialSyncCompleted = true;
        this.setChanged();
    }

    /**
     * Resolves the stored linked {@link UUID} to a valid anchored dummy.
     * An unresolved UUID is intentionally retained because it distinguishes a
     * previously BOUND pedestal from one that has never established ownership.
     */
    @Nullable
    private DamageDummyEntity resolveLinkedKeeper(
            ServerLevel level,
            BlockPos pos
    ) {
        if (this.linkedDummyUuid == null) {
            return null;
        }

        Entity resolved = level.getEntity(this.linkedDummyUuid);
        if (resolved instanceof DamageDummyEntity dummy
                && dummy.isAnchoredAt(pos)
                && !dummy.isRemoved()) {
            return dummy;
        }

        return null;
    }

    /**
     * Every non-removed {@link DamageDummyEntity} anchored at this pedestal
     * within the small local search AABB. Shared by reconciliation, fresh
     * placement and cleanup so the ownership search never diverges.
     */
    private List<DamageDummyEntity> findAnchoredDummies(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getEntitiesOfClass(
                DamageDummyEntity.class,
                searchAabb(pos),
                dummy -> dummy.isAnchoredAt(pos) && !dummy.isRemoved()
        );
    }

    /**
     * Deterministic keeper policy shared by reconciliation and fresh
     * placement: the smallest UUID string wins, so duplicate convergence is
     * stable regardless of tick order or invocation path.
     */
    private static DamageDummyEntity selectKeeper(
            List<DamageDummyEntity> anchored
    ) {
        return anchored.stream()
                .min(Comparator.comparing(
                        dummy -> dummy.getUUID().toString()
                ))
                .orElseThrow();
    }

    /** Discards every anchored dummy except the chosen keeper. */
    private static void discardDuplicatesExcept(
            List<DamageDummyEntity> anchored,
            DamageDummyEntity keeper
    ) {
        for (DamageDummyEntity duplicate : anchored) {
            if (duplicate != keeper) {
                duplicate.discardWithoutAnchorDestruction();
            }
        }
    }

    private void destroyBrokenPair(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.DAMAGE_DUMMY.get())) {
            level.destroyBlock(pos, true);
        }
    }

    private void spawnDummy(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(ModBlocks.DAMAGE_DUMMY.get())) {
            // Fail closed: no physical pedestal means no anchored dummy may be
            // spawned, even if a stale BlockEntity still executes. The state
            // is re-read here rather than trusting a cached BlockState.
            return;
        }
        if (!DamageDummyBlock.hasDummyClearance(level, pos)) {
            // Placement-API refused or the space became obstructed; retry on a
            // later reconcile cycle rather than spawning into solid collision.
            return;
        }
        DamageDummyEntity dummy = ModEntityTypes.DAMAGE_DUMMY.get()
                .create(level, EntitySpawnReason.EVENT);
        if (dummy == null) {
            return;
        }
        dummy.bindToAnchor(pos, yawFromState(current));
        if (level.addFreshEntity(dummy)) {
            this.linkedDummyUuid = dummy.getUUID();
            this.setChanged();
        }
    }

    /**
     * Test support: rewrites the persisted linked dummy {@link UUID},
     * simulating stale ownership state (a null or foreign UUID) on an
     * otherwise healthy pedestal. Never called by production lifecycle code;
     * it exists so the cleanup safety net can be exercised without
     * reflection.
     */
    public void testOverrideLinkedUuid(@Nullable UUID linkedUuid) {
        this.linkedDummyUuid = linkedUuid;
        this.setChanged();
    }

    private void positionAndOrient(
            DamageDummyEntity dummy,
            Level level,
            BlockPos pos
    ) {
        dummy.bindToAnchor(
                pos,
                yawFromState(level.getBlockState(pos))
        );
    }

    private static float yawFromState(BlockState state) {
        return state.getValue(DamageDummyBlock.FACING).toYRot();
    }

    private static AABB searchAabb(BlockPos pos) {
        return new AABB(pos).inflate(2.0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.linkedDummyUuid != null) {
            output.store(
                    KEY_LINKED_UUID,
                    UUIDUtil.CODEC,
                    this.linkedDummyUuid
            );
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.linkedDummyUuid = input
                .read(KEY_LINKED_UUID, UUIDUtil.CODEC)
                .orElse(null);
    }
}
