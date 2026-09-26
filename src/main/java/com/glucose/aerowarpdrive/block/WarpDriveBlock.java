package com.glucose.aerowarpdrive.block;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.blockentity.WarpDriveBlockEntity;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.network.AnchorListPacket;
import com.glucose.aerowarpdrive.store.SavedAnchorsDataStore;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WarpDriveBlock extends Block implements EntityBlock {
    private Component errorMessage = Component.literal("Place this on a sublevel first");
    private EnergyStorage energy;

    public WarpDriveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel) {
            if (!SableCompanion.INSTANCE.isInPlotGrid(level, pos)) {
                player.displayClientMessage(errorMessage, true);
                return InteractionResult.FAIL;
            }
            SubLevelAccess sublevel = SableCompanion.INSTANCE.getContaining(level, pos); // todo this is unused
            SavedAnchorsDataStore store = SavedAnchorsDataStore.getDimensionAnchorStore(serverLevel);

            List<WarpAnchor> anchors = store.getAnchors().stream().toList();

            AeronauticsWarpDrive.ANCHORS_LIST_SEND_CHANNEL.serverHandle(player).send(new AnchorListPacket(anchors));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WarpDriveBlockEntity(blockPos, blockState);
    }
}
