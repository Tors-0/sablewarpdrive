package com.glucose.aerowarpdrive.block;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.blockentity.WarpDriveBlockEntity;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.core.WarpDriveStates;
import com.glucose.aerowarpdrive.network.AnchorListPacket;
import com.glucose.aerowarpdrive.store.SavedAnchorsDataStore;
import com.glucose.aerowarpdrive.util.Geometry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_DRIVE_BLOCK_ENTITY;

public class WarpDriveBlock extends Block implements EntityBlock {
    public static final Component errorMessage = Component.translatable("message.aerowarpdrive.error_sublevel_needed");

    public static final Property<WarpDriveStates> STATUS = EnumProperty.create("status", WarpDriveStates.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");

    public WarpDriveBlock(Properties properties) {
        super(properties);

        registerDefaultState(getStateDefinition().any()
                .setValue(STATUS, WarpDriveStates.WAITING)
                .setValue(FACING, Direction.NORTH)
                .setValue(ASSEMBLED, false)
        );
    }

    private static Vec3i coreCoreOffset = new Vec3i(2,2,0);
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!state.getValue(ASSEMBLED)) return;
        Vec3 pos2 = Sable.HELPER.projectOutOfSubLevel(level, pos.getCenter());
        pos2 = pos2.add(Vec3.atCenterOf(Geometry.rotatePosition(coreCoreOffset,state.getValue(FACING))));

        for (Vec3i blockPos : ((WarpDriveBlockEntity) level.getBlockEntity(pos)).getCorePositions()) {
            blockPos = Geometry.rotatePosition(blockPos, state.getValue(FACING));
            if (random.nextInt(16) == 0) {
                level.addParticle(ParticleTypes.ENCHANT, pos2.x() - 1, pos2.y() + 1, pos2.z() - 0.5, (double) ((float) blockPos.getX() + random.nextFloat()) - 0.5, (double) ((float) blockPos.getY() - random.nextFloat() - 1.0F), (double) ((float) blockPos.getZ() + random.nextFloat()) - 0.5);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS, FACING, ASSEMBLED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // We use a second method here due to generic conversions
    // If extending `BaseEntityBlock`, this method is also available there as a protected static method
    private static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> checkedType, BlockEntityTicker<? super E> ticker
    ) {
        return checkedType == type ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // You can return different tickers here, depending on whatever factors you want. A common use case would be
        // to return different tickers on the client or server, only tick one side to begin with,
        // or only return a ticker for some blockstates (e.g. when using a "my machine is working" blockstate property).
        if (!(level instanceof ServerLevel))
            return createTickerHelper(type, WARP_DRIVE_BLOCK_ENTITY.get(), WarpDriveBlockEntity::tick);
        else return null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!(newState.getBlock() instanceof WarpDriveBlock))
            ((WarpDriveBlockEntity)level.getBlockEntity(pos)).onControllerBroken();

        // removes the block entity fr
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @NotNull
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel) {
            if (!SableCompanion.INSTANCE.isInPlotGrid(level, pos)) {
                player.displayClientMessage(errorMessage, true);
                return InteractionResult.FAIL;
            }
            WarpDriveBlockEntity blockEntity = (WarpDriveBlockEntity)level.getBlockEntity(pos);
            assert blockEntity != null;

            var wasAssembled = state.getValue(ASSEMBLED);

            if (!wasAssembled) {
                var corePlaced = blockEntity.tryPlaceNextCore(player);
                if (corePlaced) return InteractionResult.SUCCESS;
            }

            var isAssembled = blockEntity.initMultiblock(state);
            blockEntity.rescanMultiblock();

            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.aerowarpdrive.warp_drive.missing_core"));
                return InteractionResult.SUCCESS;
            }

            String message = blockEntity.getStatusMessage();

            // get all rift anchors in this dimension
            List<WarpAnchor> anchors;
            if (state.getValue(STATUS) == WarpDriveStates.WAITING) {
                SavedAnchorsDataStore store = SavedAnchorsDataStore.getDimensionAnchorStore(serverLevel);
                anchors = store.getAnchors().stream().toList();
            } else {
                anchors = List.of();
            }
            // send them to the client
            AeronauticsWarpDrive.ANCHORS_LIST_SEND_CHANNEL.serverHandle(player).send(new AnchorListPacket(anchors, pos, message));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WarpDriveBlockEntity(blockPos, blockState);
    }
}
