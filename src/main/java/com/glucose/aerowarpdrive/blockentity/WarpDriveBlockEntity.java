package com.glucose.aerowarpdrive.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_DRIVE_BLOCK_ENTITY;

public class WarpDriveBlockEntity extends BlockEntity {
    private final EnergyStorage energyStorage = createEnergyStorage();

    public WarpDriveBlockEntity(BlockPos pos, BlockState blockState) {
        super(WARP_DRIVE_BLOCK_ENTITY.get(), pos, blockState);
    }

    private EnergyStorage createEnergyStorage() {
        return new EnergyStorage(50_000_000);
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return energyStorage;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.deserializeNBT(registries, tag.get("energy_stored"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy_stored",energyStorage.serializeNBT(registries));
    }
}
