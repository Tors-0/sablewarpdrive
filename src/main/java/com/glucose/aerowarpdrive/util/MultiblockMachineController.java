package com.glucose.aerowarpdrive.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.glucose.aerowarpdrive.block.DriveCoreBlock;
import com.glucose.aerowarpdrive.block.WarpDriveBlock;
import com.glucose.aerowarpdrive.blockentity.DriveCoreBlockEntity;
import com.glucose.aerowarpdrive.client.init.ParticleContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;

// lawfully copied code from https://github.com/rearth/Oritech as per the terms of the CC0 1.0 Universal license
public interface MultiblockMachineController extends MachineControllerLifecycle {

    List<Vec3i> getCorePositions();

    Direction getFacingForMultiblock();

    BlockPos getPosForMultiblock();

    Level getWorldForMultiblock();

    ArrayList<BlockPos> getConnectedCores();

    EnergyStorage getEnergyStorageForMultiblock(Direction direction);

    default void addMultiblockToNbt(CompoundTag nbt) {

        var posList = new ListTag();
        for (var pos : getConnectedCores()) {
            var offset = RelativePosition.toOffset(pos, getPosForMultiblock());
            var posTag = new CompoundTag();
            posTag.putInt("x", offset.getX());
            posTag.putInt("y", offset.getY());
            posTag.putInt("z", offset.getZ());
            posList.add(posTag);
        }
        nbt.put("connectedCores", posList);
    }

    default void loadMultiblockNbtData(CompoundTag nbt) {

        var posList = nbt.getList("connectedCores", Tag.TAG_COMPOUND);
        var coreBlocksConnected = getConnectedCores();
        coreBlocksConnected.clear();

        for (var posTag : posList) {
            var posCompound = (CompoundTag) posTag;
            var x = posCompound.getInt("x");
            var y = posCompound.getInt("y");
            var z = posCompound.getInt("z");
            var pos = RelativePosition.toWorldPosition(new BlockPos(x, y, z), getPosForMultiblock());
            coreBlocksConnected.add(pos);
        }
    }

    default Boolean tryPlaceNextCore(Player player) {

        var heldStack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        var heldItem = heldStack.getItem();

        if (!(heldItem instanceof BlockItem blockItem)) return false;

        if (blockItem.getBlock() instanceof DriveCoreBlock) {
            var nextPosition = this.getNextMissingCore();
            if (nextPosition != null) {
                this.getWorldForMultiblock().setBlockAndUpdate(nextPosition, blockItem.getBlock().defaultBlockState());
                if (!player.isCreative()) {
                    heldStack.shrink(1);
                    if (heldStack.getCount() == 0)
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    default BlockPos getNextMissingCore() {

        var world = getWorldForMultiblock();
        var pos = getPosForMultiblock();

        var ownFacing = getFacingForMultiblock();
        var targetMachinePositions = getCorePositions();

        for (var targetMachinePosition : targetMachinePositions) {
            var rotatedPos = Geometry.rotatePosition(targetMachinePosition, ownFacing);
            var checkPos = pos.offset(rotatedPos);
            var checkState = Objects.requireNonNull(world).getBlockState(checkPos);

            if (checkState.is(Blocks.AIR) || checkState.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "replaceable")))) {
                return checkPos;
            }
        }

        return null;
    }

    default boolean initMultiblock(BlockState state) {

        // check if multiblock is already created, if so cancel
        // call method the get a list of relative positions
        // check all positions if the blocks there extend MachineCoreBlock
        // if so, add them to list of used blocks
        // if not (e.g. block wrong type or air), draw a small particle to indicate the missing position
        // when all blocks are valid, multiblock is active
        // update all multiblocks state to USED=true, write controller position to block state

        if (state.getValue(WarpDriveBlock.ASSEMBLED)) return true;
        var world = getWorldForMultiblock();
        var pos = getPosForMultiblock();
        var coreBlocksConnected = getConnectedCores();

        var ownFacing = getFacingForMultiblock();

        var targetMachinePositions = getCorePositions();
        var coreBlocks = new ArrayList<MultiBlockElement>(targetMachinePositions.size());

        for (var targetMachinePosition : targetMachinePositions) {
            var rotatedPos = Geometry.rotatePosition(targetMachinePosition, ownFacing);
            var checkPos = pos.offset(rotatedPos);
            var checkState = Objects.requireNonNull(world).getBlockState(checkPos);

            var blockType = checkState.getBlock();
            if (blockType instanceof DriveCoreBlock coreBlock && !checkState.getValue(DriveCoreBlock.USED)) {
                coreBlocks.add(new MultiBlockElement(checkState, coreBlock, checkPos));
            } else {
                highlightBlock(checkPos, world);
            }
        }

        if (targetMachinePositions.size() == coreBlocks.size()) {
            // valid
            for (var core : coreBlocks) {
                var coreEntity = (DriveCoreBlockEntity) world.getBlockEntity(core.pos());
                coreEntity.setControllerPos(pos);

                world.setBlockAndUpdate(core.pos(), core.state().setValue(DriveCoreBlock.USED, true));
                coreBlocksConnected.add(core.pos());
            }

            Objects.requireNonNull(world).setBlockAndUpdate(pos, state.setValue(WarpDriveBlock.ASSEMBLED, true));
            return true;
        } else {
            // invalid
            return false;
        }
    }

    // Rebuild links after a controller and its structure have been relocated together.
    default void rescanMultiblock() {
        var world = getWorldForMultiblock();
        var pos = getPosForMultiblock();
        if (world == null || !world.hasChunkAt(pos)) return;

        var coreBlocks = new ArrayList<MultiBlockElement>(getCorePositions().size());

        for (var targetMachinePosition : getCorePositions()) {
            var corePos = pos.offset(Geometry.rotatePosition(targetMachinePosition, getFacingForMultiblock()));
            var coreState = world.getBlockState(corePos);
            if (!(coreState.getBlock() instanceof DriveCoreBlock coreBlock)
                    || !(world.getBlockEntity(corePos) instanceof DriveCoreBlockEntity coreEntity)) {
                resetInvalidMultiblock();
                return;
            }

            if (coreState.getValue(DriveCoreBlock.USED) && !coreEntity.getControllerPos().equals(pos)) {
                var linkedController = world.getBlockEntity(coreEntity.getControllerPos());
                if (linkedController instanceof MultiblockMachineController) {
                    resetInvalidMultiblock();
                    return;
                }
            }

            coreBlocks.add(new MultiBlockElement(coreState, coreBlock, corePos));
        }

        if (coreBlocks.isEmpty()) {
            resetInvalidMultiblock();
            return;
        }

        var connectedCores = getConnectedCores();
        connectedCores.clear();
        for (var core : coreBlocks) {
            var coreEntity = (DriveCoreBlockEntity) world.getBlockEntity(core.pos());
            coreEntity.setControllerPos(pos);
            if (!core.state().getValue(DriveCoreBlock.USED))
                world.setBlockAndUpdate(core.pos(), core.state().setValue(DriveCoreBlock.USED, true));
            connectedCores.add(core.pos());
        }

        var state = world.getBlockState(pos);
        if (state.hasProperty(WarpDriveBlock.ASSEMBLED) && !state.getValue(WarpDriveBlock.ASSEMBLED))
            world.setBlockAndUpdate(pos, state.setValue(WarpDriveBlock.ASSEMBLED, true));

        // Preserve controller-specific refresh work without cycling any block states.
        initMultiblock(world.getBlockState(pos));
    }

    private void resetInvalidMultiblock() {
        var world = getWorldForMultiblock();
        var pos = getPosForMultiblock();
        if (world == null) return;

        for (var targetMachinePosition : getCorePositions()) {
            var corePos = pos.offset(Geometry.rotatePosition(targetMachinePosition, getFacingForMultiblock()));
            var coreState = world.getBlockState(corePos);
            if (!(coreState.getBlock() instanceof DriveCoreBlock) || !coreState.getValue(DriveCoreBlock.USED)
                    || !(world.getBlockEntity(corePos) instanceof DriveCoreBlockEntity coreEntity))
                continue;

            var linkedController = world.getBlockEntity(coreEntity.getControllerPos());
            if (coreEntity.getControllerPos().equals(pos) || !(linkedController instanceof MultiblockMachineController))
                world.setBlockAndUpdate(corePos, coreState.setValue(DriveCoreBlock.USED, false));
        }

        getConnectedCores().clear();
        var state = world.getBlockState(pos);
        if (state.hasProperty(WarpDriveBlock.ASSEMBLED) && state.getValue(WarpDriveBlock.ASSEMBLED))
            world.setBlockAndUpdate(pos, state.setValue(WarpDriveBlock.ASSEMBLED, false));
    }

    default void onCoreBroken(BlockPos corePos) {

        var world = getWorldForMultiblock();
        var pos = getPosForMultiblock();
        var coreBlocksConnected = getConnectedCores();

        Objects.requireNonNull(world).setBlockAndUpdate(pos, world.getBlockState(pos).setValue(WarpDriveBlock.ASSEMBLED, false));

        for (var core : coreBlocksConnected) {
            if (core.equals(corePos)) continue;

            var state = world.getBlockState(core);
            if (state.getBlock() instanceof DriveCoreBlock) {
                world.setBlockAndUpdate(core, state.setValue(DriveCoreBlock.USED, false));
            }
        }

        coreBlocksConnected.clear();
    }

    default void onControllerBroken() {

        var world = getWorldForMultiblock();
        var coreBlocksConnected = getConnectedCores();

        for (var core : coreBlocksConnected) {
            var state = Objects.requireNonNull(world).getBlockState(core);
            if (state.getBlock() instanceof DriveCoreBlock) {
                world.setBlockAndUpdate(core, state.setValue(DriveCoreBlock.USED, false));
            }
        }

        coreBlocksConnected.clear();
    }

    private void highlightBlock(BlockPos block, Level world) {
        ParticleContent.HighlightBlock(world, Vec3.atLowerCornerOf(block));
    }

    record MultiBlockElement(BlockState state, DriveCoreBlock coreBlock, BlockPos pos) {
    }

}