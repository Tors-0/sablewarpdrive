package com.glucose.aerowarpdrive.blockentity;

import com.glucose.aerowarpdrive.block.DriveCoreBlock;
import com.glucose.aerowarpdrive.util.MultiblockMachineController;
import com.glucose.aerowarpdrive.util.RelativePosition;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.DRIVE_CORE_BLOCK_ENTITY;

public class DriveCoreBlockEntity extends BlockEntity {

    private BlockPos controllerOffset = BlockPos.ZERO;
    private MultiblockMachineController controllerEntity;

    public DriveCoreBlockEntity(BlockPos pos, BlockState state) {
        super(DRIVE_CORE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putInt("controller_x", controllerOffset.getX());
        nbt.putInt("controller_y", controllerOffset.getY());
        nbt.putInt("controller_z", controllerOffset.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        var storedPosition = new BlockPos(nbt.getInt("controller_x"), nbt.getInt("controller_y"), nbt.getInt("controller_z"));
        controllerOffset = RelativePosition.normalizeOffset(storedPosition, worldPosition);
    }

    public BlockPos getControllerPos() {
        return worldPosition.offset(controllerOffset);
    }

    public void setControllerPos(BlockPos controllerPos) {
        var newOffset = RelativePosition.toOffset(controllerPos, worldPosition);
        if (controllerOffset.equals(newOffset)) return;

        this.controllerOffset = newOffset;
        this.controllerEntity = null;    // forces cache reload
        this.setChanged();
    }

    @Nullable
    public MultiblockMachineController getCachedController() {
        if (level == null || !this.getBlockState().getValue(DriveCoreBlock.USED)) return null;

        if (controllerEntity == null || ((BlockEntity) controllerEntity).isRemoved()) {
            var candidate = Objects.requireNonNull(level).getBlockEntity(getControllerPos());
            if (candidate instanceof MultiblockMachineController controller) {
                controllerEntity = controller;
            } else {
                controllerEntity = null;
            }
        }

        return controllerEntity;
    }

    @Nullable
    private EnergyStorage getMainEnergyStorage(Direction direction) {

        var isUsed = this.getBlockState().getValue(DriveCoreBlock.USED);
        if (!isUsed) return new EnergyStorage(0);

        var controllerEntity = getCachedController();
        if (controllerEntity == null) return new EnergyStorage(0);    // this should never happen
        return controllerEntity.getEnergyStorageForMultiblock(direction);
    }

    public boolean isEnabled() {
        return this.getBlockState().getValue(DriveCoreBlock.USED);
    }

    public EnergyStorage getEnergyStorage(Direction direction) {
        return getMainEnergyStorage(direction);
    }
}