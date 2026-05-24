package io.github.naimjeg.damagenexus.menu;

import io.github.naimjeg.damagenexus.block.entity.DamageDummyBlockEntity;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeService;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeSnapshot;
import io.github.naimjeg.damagenexus.registry.ModBlocks;
import io.github.naimjeg.damagenexus.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Slot-free management menu for a {@link DamageDummyBlockEntity} pedestal.
 *
 * <p>There are no slots or inventory storage. Its snapshot is presentation
 * state only; the linked entity's real AttributeMap remains authoritative.</p>
 */
public class DamageDummyMenu extends AbstractContainerMenu {

    private final BlockPos anchorPos;
    private final ContainerLevelAccess access;
    private DamageDummyAttributeSnapshot snapshot;
    private long snapshotVersion;

    /**
     * Server constructor, invoked by
     * {@link DamageDummyBlockEntity#createMenu}. The block entity is always
     * placed in a level on this path; a detached instance falls back to
     * {@link ContainerLevelAccess#NULL}, which makes the menu immediately
     * invalid.
     */
    public DamageDummyMenu(
            int containerId,
            Inventory inventory,
            DamageDummyBlockEntity blockEntity
    ) {
        super(ModMenuTypes.DAMAGE_DUMMY.get(), containerId);
        this.anchorPos = blockEntity.getBlockPos().immutable();
        Level level = blockEntity.getLevel();
        this.access = level != null
                ? ContainerLevelAccess.create(level, this.anchorPos)
                : ContainerLevelAccess.NULL;
        this.snapshot = level instanceof net.minecraft.server.level.ServerLevel
                serverLevel
                ? DamageDummyAttributeService.snapshot(serverLevel, blockEntity)
                : DamageDummyAttributeSnapshot.unavailable(this.anchorPos);
    }

    /**
     * Client constructor, invoked by the {@link ModMenuTypes#DAMAGE_DUMMY}
     * menu type with the opening data written by
     * {@link DamageDummyBlockEntity#writeClientSideData}. The buffer always
     * contains the anchor {@link BlockPos}; the null guard is defensive only.
     */
    public DamageDummyMenu(
            int containerId,
            Inventory inventory,
            RegistryFriendlyByteBuf extraData
    ) {
        super(ModMenuTypes.DAMAGE_DUMMY.get(), containerId);
        this.anchorPos = extraData != null
                ? extraData.readBlockPos().immutable()
                : BlockPos.ZERO;
        this.snapshot = extraData != null
                ? DamageDummyAttributeSnapshot.STREAM_CODEC.decode(extraData)
                : DamageDummyAttributeSnapshot.unavailable(this.anchorPos);
        if (!this.snapshot.anchorPos().equals(this.anchorPos)) {
            this.snapshot = DamageDummyAttributeSnapshot.unavailable(
                    this.anchorPos
            );
        }
        this.access = ContainerLevelAccess.create(
                inventory.player.level(),
                this.anchorPos
        );
    }

    /** The pedestal position this menu manages. */
    public BlockPos anchorPos() {
        return this.anchorPos;
    }

    public DamageDummyAttributeSnapshot snapshot() {
        return this.snapshot;
    }

    public long snapshotVersion() {
        return this.snapshotVersion;
    }

    /** Replaces presentation state only when it belongs to this exact menu. */
    public boolean replaceSnapshot(DamageDummyAttributeSnapshot snapshot) {
        if (snapshot == null
                || !this.anchorPos.equals(snapshot.anchorPos())) {
            return false;
        }
        this.snapshot = snapshot;
        this.snapshotVersion++;
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        // No slots exist, so there is nothing to quick-move.
        return ItemStack.EMPTY;
    }

    /**
     * The menu stays open only while the pedestal block still exists at the
     * anchor position and the player remains within the normal block
     * interaction distance (vanilla distance helper, no custom permission
     * logic).
     */
    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.DAMAGE_DUMMY.get());
    }
}
