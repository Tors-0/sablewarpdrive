package com.glucose.aerowarpdrive.block;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.blockentity.WarpDriveBlockEntity;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.core.WarpDriveStates;
import com.glucose.aerowarpdrive.network.AnchorListPacket;
import com.glucose.aerowarpdrive.store.SavedAnchorsDataStore;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WarpDriveBlock extends Block implements EntityBlock {
    public static final Component errorMessage = Component.translatable("string.aerowarpdrive.error_sublevel_needed");

    public static final Property<WarpDriveStates> STATUS = EnumProperty.create("status", WarpDriveStates.class);

    public WarpDriveBlock(Properties properties) {
        super(properties);

        registerDefaultState(getStateDefinition().any()
                .setValue(STATUS, WarpDriveStates.WAITING)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
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

            String message = blockEntity != null ? blockEntity.getStatusMessage() : "Error";

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

    public static final VoxelShape blockShape = Shapes.join(
            Shapes.box(0,0,0,1,0.125,1),
            Shapes.box(0.0625,0.125,0.0625,0.9375,1,0.9375),
            BooleanOp.OR
    );
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return blockShape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WarpDriveBlockEntity(blockPos, blockState);
    }
}
