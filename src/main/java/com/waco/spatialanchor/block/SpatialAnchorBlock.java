package com.waco.spatialanchor.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelReader;


/**
 * 空間アンカーブロック
 *
 * <p>Create の KineticBlock を継承し、応力を消費することで
 * Create Aeronautics の飛行船を起動時座標に固定する。</p>
 *
 * <p>BlockState プロパティ:</p>
 * <ul>
 *   <li>FACING  : ギアとの接続方向（軸方向）</li>
 *   <li>ACTIVE  : アンカー有効/無効</li>
 * </ul>
 */
public class SpatialAnchorBlock extends KineticBlock implements IBE<SpatialAnchorBlockEntity> {

    // ── BlockState ──────────────────────────────────────────────────────
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty   ACTIVE  = BooleanProperty.create("active");

    // ── 形状 ─────────────────────────────────────────────────────────────
    // 中心にコアを持つ 14x14x14 の箱形状
    private static final VoxelShape SHAPE = Block.box(1, 1, 1, 15, 15, 15);

    // ── Codec（Create 6.x 必須）─────────────────────────────────────────
    public static final MapCodec<SpatialAnchorBlock> CODEC = simpleCodec(SpatialAnchorBlock::new);

    @Override
    protected MapCodec<? extends SpatialAnchorBlock> codec() {
        return CODEC;
    }

    // ── コンストラクタ ────────────────────────────────────────────────────
    public SpatialAnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.UP)
                        .setValue(ACTIVE, false)
        );
    }

    // ── BlockState 定義 ───────────────────────────────────────────────────
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // プレイヤーの視線反対方向をFACINGに
        Direction facing = ctx.getNearestLookingDirection().getOpposite();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ACTIVE, false);
    }

    // ── 形状 ─────────────────────────────────────────────────────────────
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext ctx) {
        return SHAPE;
    }

    // ── KineticBlock: 軸方向 ─────────────────────────────────────────────
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        // FACING 方向と逆方向にシャフト接続口を持つ
        return face == state.getValue(FACING) ||
                face == state.getValue(FACING).getOpposite();
    }

    // ── 右クリック: 状態確認 ─────────────────────────────────────────────
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        return onBlockEntityUse(level, pos, be -> {
            be.sendStatusMessage(player);
            return InteractionResult.SUCCESS;
        });
    }

    // ── ブロック破壊時: アンカー解除 ─────────────────────────────────────
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            withBlockEntityDo(level, pos, SpatialAnchorBlockEntity::deactivate);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ── ブロック破壊直前: 先に拘束を解除 ─────────────────────────────────
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // FixedConstraint が船を物理固定したままだと破壊が成立しないため、
        // 破壊処理に入る前にアンカーを解除して拘束を外す
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, SpatialAnchorBlockEntity::deactivate);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ── IBE 実装 ─────────────────────────────────────────────────────────
    @Override
    public Class<SpatialAnchorBlockEntity> getBlockEntityClass() {
        return SpatialAnchorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SpatialAnchorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SPATIAL_ANCHOR.get();
    }
}
