package com.glucose.aerowarpdrive.block;

import com.glucose.aerowarpdrive.blockentity.WarpAnchorBlockEntity;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.store.SavedAnchorsDataStore;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_ANCHOR;

public class WarpAnchorBlock extends Block implements EntityBlock, BlockSubLevelAssemblyListener {
    public static final Component errorMessage = Component.translatable("message.aerowarpdrive.error_sublevel_banned");

    public static final Property<Boolean> ACTIVATED = BooleanProperty.create("activated");

    public WarpAnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(ACTIVATED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVATED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new WarpAnchorBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // remove this anchor from the dimensional list
        if (level instanceof ServerLevel serverLevel) {
            if (!newState.getBlock().equals(WARP_ANCHOR.get())) {
                WarpAnchor anchor = ((WarpAnchorBlockEntity) serverLevel.getBlockEntity(pos)).getAnchor();
                SavedAnchorsDataStore.getDimensionAnchorStore(serverLevel).removeAnchor(anchor);
            }
        }

        // deletes the block entity fr
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // gotta prevent from working on a sublevel
        if (SableCompanion.INSTANCE.isInPlotGrid(level, pos)) {
            player.displayClientMessage(errorMessage, true);
            return ItemInteractionResult.FAIL;
        }

        // lets eat an eye of ender before registering as a valid anchor
        if (stack.is(Items.ENDER_EYE)) {
            if (level.getBlockEntity(pos) instanceof WarpAnchorBlockEntity anchorBlockEntity) {
                if (!anchorBlockEntity.isActivated()) {
                    anchorBlockEntity.activate();
                    stack.consume(1,null);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.FAIL;
    }

    @Override
    public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
        resultingLevel.setBlock(newPos, newState.setValue(ACTIVATED, false), UPDATE_CLIENTS);

        // remove this anchor from the dimensional list
        WarpAnchor anchor = ((WarpAnchorBlockEntity) resultingLevel.getBlockEntity(newPos)).getAnchor();
        SavedAnchorsDataStore.getDimensionAnchorStore(resultingLevel).removeAnchor(anchor);
    }
}
