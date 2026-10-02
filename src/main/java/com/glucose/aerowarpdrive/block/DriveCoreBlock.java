package com.glucose.aerowarpdrive.block;

import com.glucose.aerowarpdrive.blockentity.DriveCoreBlockEntity;
import com.glucose.aerowarpdrive.util.MultiblockMachineController;
import net.minecraft.world.level.LevelAccessor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class DriveCoreBlock extends Block implements EntityBlock {

    public static final BooleanProperty USED = BooleanProperty.create("core_used");

    public DriveCoreBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(USED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(USED);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        tooltip.add(Component.translatable("tooltip.aerowarpdrive.core_block").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, options);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        onBlockRemoved(state, world, pos);
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        onBlockRemoved(state, level, pos);
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        onBlockRemoved(state, level, pos);
        super.destroy(level, pos, state);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {

        if (state.getValue(USED)) {
            onBlockRemoved(state, world, pos);
        }

        super.onExplosionHit(state, world, pos, explosion, stackMerger);
    }

    private static void onBlockRemoved(BlockState state, LevelAccessor world, BlockPos pos) {
        if (!world.isClientSide() && state.getValue(USED) && world.getBlockEntity(pos) instanceof DriveCoreBlockEntity coreEntity) {
            var controllerPos = coreEntity.getControllerPos();
            if (controllerPos != null && world.getBlockEntity(controllerPos) instanceof MultiblockMachineController machineEntity) {
                machineEntity.onCoreBroken(pos);
            }
        }
    }

    @NotNull
    public static BlockPos getControllerPos(LevelAccessor world, BlockPos pos) {
        var coreEntity = (DriveCoreBlockEntity) world.getBlockEntity(pos);
        return Objects.requireNonNull(coreEntity).getControllerPos();
    }

    @Nullable
    public static BlockEntity getControllerEntity(LevelAccessor world, BlockPos pos) {
        return world.getBlockEntity(getControllerPos(world, pos));
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {

        if (!state.getValue(USED)) return InteractionResult.PASS;

        if (!world.isClientSide) {
            var controllerPos = getControllerPos(world, pos);
            var controllerBlock = world.getBlockState(controllerPos);

            return controllerBlock.useWithoutItem(world, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), controllerPos, hit.isInside()));
            
        }

        return InteractionResult.SUCCESS;

    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new DriveCoreBlockEntity(pos, state);
    }
}