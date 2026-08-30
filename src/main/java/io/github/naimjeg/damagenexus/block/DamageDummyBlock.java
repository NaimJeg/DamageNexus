package io.github.naimjeg.damagenexus.block;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.block.entity.DamageDummyBlockEntity;
import io.github.naimjeg.damagenexus.entity.DamageDummyEntity;
import io.github.naimjeg.damagenexus.registry.ModBlockEntityTypes;
import io.github.naimjeg.damagenexus.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Physical pedestal for the damage dummy.
 *
 * <p>This block is a low-profile 14x1x14 pressure-plate-like base. It owns
 * one half of the anchor lifecycle through its
 * {@link DamageDummyBlockEntity}; the real
 * combat target is the {@code DamageDummyEntity} standing directly on top of
 * the plate. The block itself has a fixed collision/selection shape so the
 * player can aim at and mine the pedestal independently of the entity, and
 * right-clicking the plate opens the damage dummy management menu.</p>
 *
 * <p>The horizontal {@code FACING} state only orients the upper entity; a
 * facing rewrite of the same block never destroys/recreates the linked
 * entity. There is no powered state, no redstone behavior and no stepping
 * activation: the plate is only visually pressure-plate-like.</p>
 */
public class DamageDummyBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Single authoritative height of the pedestal plate in block units
     * (1/16 of a block). The anchored dummy's feet are placed at exactly this
     * height above the anchor block; never duplicate {@code 0.0625},
     * {@code 1 / 16} or {@code 0.5} anywhere else.
     */
    public static final double BASE_HEIGHT = 1.0D / 16.0D;

    /** Low-profile plate: 14x1x14, centered horizontally in the block. */
    private static final VoxelShape SHAPE = Block.box(
            1.0D, 0.0D, 1.0D,
            15.0D, 1.0D, 15.0D
    );

    public DamageDummyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(DamageDummyBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    /**
     * Placement fails when the full entity volume above the plate is
     * obstructed, so the pedestal never silently spawns a dummy intersecting
     * a ceiling. Existing blocks are never removed to make room.
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!hasDummyClearance(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return this.defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    /**
     * Prospective bounding box of the anchored dummy when standing on the
     * pedestal at {@code pos}: feet at the single authoritative
     * {@link DamageDummyEntity#getAnchoredPosition(BlockPos)} (centered X/Z,
     * base height above the anchor block), sized from the real
     * {@code EntityType} dimensions. The anchor position comes from the
     * single authoritative method so placement always agrees with the
     * entity's actual spawn position.
     */
    public static boolean hasDummyClearance(Level level, BlockPos pos) {
        AABB dummyBox = ModEntityTypes.DAMAGE_DUMMY.get()
                .getDimensions()
                .makeBoundingBox(DamageDummyEntity.getAnchoredPosition(pos));
        return level.noCollision(dummyBox);
    }

    /**
     * Fresh normal BlockItem placement callback (26.1.2):
     * {@link BlockItem} calls this immediately after {@code level.setBlock}
     * has placed the pedestal, so the block entity is already present and
     * guaranteed queryable here. This is the single synchronous hook that
     * turns a normal player placement into one atomic-looking placement
     * action: the anchored dummy is initialized right away on the server
     * instead of waiting for the first periodic reconciliation.
     *
     * <p>Direct world mutations ({@code /setblock}, structure placement,
     * {@code GameTestHelper#setBlock}) never reach this callback and remain
     * covered by periodic reconciliation; this method intentionally does not
     * couple the block to every world mutation source. The client-side
     * placement prediction also runs this hook, but it is a no-op there: the
     * entity is created only on a {@link ServerLevel}.</p>
     */
    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DamageDummyBlockEntity dummyBlockEntity) {
            dummyBlockEntity.initializeFreshPlacement(serverLevel);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DamageDummyBlockEntity(pos, state);
    }

    /**
     * Right-click entry point for the damage dummy management menu. Both an
     * empty hand and a hand holding an ordinary item reach this method:
     * {@link #useItemOn} intentionally keeps its 26.1.2 default
     * {@link InteractionResult#TRY_WITH_EMPTY_HAND}, which routes a main-hand
     * interaction to this empty-hand path after the block-use stage. The
     * interaction is consumed, so the held item is never placed/used and the
     * menu opens exactly once. Sneak semantics remain vanilla: sneaking while
     * holding an item suppresses the block use and uses the item instead.
     */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide()) {
            MenuProvider menuProvider = state.getMenuProvider(level, pos);
            if (menuProvider != null) {
                player.openMenu(menuProvider);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
                type,
                ModBlockEntityTypes.DAMAGE_DUMMY.get(),
                DamageDummyBlockEntity::serverTick
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    /**
     * Explicit model rendering: the pedestal is a normal block model (the
     * upper dummy is rendered by the entity renderer, never by a block entity
     * renderer).
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
