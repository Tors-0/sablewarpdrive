package com.glucose.aerowarpdrive.blockentity;

import com.glucose.aerowarpdrive.block.WarpDriveBlock;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.core.WarpDriveStates;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.command.data_accessor.SubLevelDataAccessor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.joml.*;

import java.lang.Math;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_DRIVE_BLOCK_ENTITY;
import static com.glucose.aerowarpdrive.core.WarpDriveStates.*;

public class WarpDriveBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private final EnergyStorage energyStorage = createEnergyStorage();
    private int consumptionPerCharge;
    private int chargingTicksRemaining;
    private int cooldownTicksRemaining;
    private String statusMessage = "";
    private BlockPos target;

    public WarpDriveBlockEntity(BlockPos pos, BlockState blockState) {
        super(WARP_DRIVE_BLOCK_ENTITY.get(), pos, blockState);
    }

    private EnergyStorage createEnergyStorage() {
        return new EnergyStorage(50_000_000);
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return energyStorage;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void beginCharge(WarpAnchor target) {
        this.target = target.getPos();
        double distance = Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(getLevel(), this.target.getCenter(), this.getBlockPos().getCenter()));
        double totalMass = 0;
        for (SubLevel subLevel : SubLevelHelper.getConnectedChain(Sable.HELPER.getContaining(this))) {
            totalMass += ((ServerSubLevel) subLevel).getMassTracker().getMass();
        }
        double energyCost = totalMass * distance;
        int ticks = (int) (distance / 50);
        this.chargingTicksRemaining = ticks;
        this.consumptionPerCharge = (int) (energyCost / ticks);
        this.cooldownTicksRemaining = (int) (distance / 20);
        getLevel().setBlock(getBlockPos(), getBlockState().setValue(WarpDriveBlock.STATUS, READY), WarpDriveBlock.UPDATE_CLIENTS);
        this.statusMessage = "Ready to charge.\nWarp Cost: " + (int) energyCost + " FE.\nPower to begin charging";
    }

    private void teleportSubLevel(ServerSubLevel subLevel) {
        Vec3 position = (target.getBottomCenter()).add(0, (subLevel.boundingBox().size().y / 2) + 1, 0);
        SubLevelContainer.getContainer(subLevel.getLevel()).physicsSystem().getPipeline().teleport(
                subLevel,
                new Vector3d(position.x, position.y, position.z),
                subLevel.logicalPose().orientation()
        );
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        switch (getBlockState().getValue(WarpDriveBlock.STATUS)) {
            case READY -> {
                // destination selected, consuming power
                if (getLevel().getDirectSignalTo(getBlockPos()) > 0) {
                    // start charging
                    this.statusMessage = "Charging at " + consumptionPerCharge + " FE/t";
                    getLevel().setBlock(getBlockPos(), getBlockState().setValue(WarpDriveBlock.STATUS, CHARGING), WarpDriveBlock.UPDATE_CLIENTS);
                }
            }
            case CHARGING -> {
                if (this.chargingTicksRemaining <= 0) {
                    level.setBlock(getBlockPos(), getBlockState().setValue(WarpDriveBlock.STATUS, COOLING), WarpDriveBlock.UPDATE_CLIENTS);

                    teleportSubLevel(subLevel);

                    this.consumptionPerCharge = 0;
                    this.statusMessage = "Warp Complete";
                } else {
                    int consumedPower = this.getEnergyStorage(Direction.UP).extractEnergy(this.consumptionPerCharge, false);
                    if (consumedPower < this.consumptionPerCharge) {
                        this.chargingTicksRemaining = 0;
                        this.consumptionPerCharge = 0;
                        level.setBlock(getBlockPos(), getBlockState().setValue(WarpDriveBlock.STATUS, COOLING), WarpDriveBlock.UPDATE_CLIENTS);
                        this.cooldownTicksRemaining = 600;
                        this.statusMessage = "Charge failed, cooling down";
                    } else {
                        this.chargingTicksRemaining--;
                    }
                }
            }
            case COOLING -> {
                // warp complete, timer to next warp
                if (this.cooldownTicksRemaining <= 0) {
                    level.setBlock(getBlockPos(), getBlockState().setValue(WarpDriveBlock.STATUS, WAITING), WarpDriveBlock.UPDATE_CLIENTS);
                } else {
                    this.cooldownTicksRemaining--;
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.deserializeNBT(registries, tag.get("energy_stored"));
        consumptionPerCharge = tag.getInt("consumptionPerCharge");
        chargingTicksRemaining = tag.getInt("chargingTicks");
        cooldownTicksRemaining = tag.getInt("cooldownTicks");
        statusMessage = tag.getString("statusMessage");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy_stored",energyStorage.serializeNBT(registries));
        tag.putInt("consumptionPerCharge", consumptionPerCharge);
        tag.putInt("chargingTicks", chargingTicksRemaining);
        tag.putInt("cooldownTicks", cooldownTicksRemaining);
        tag.putString("statusMessage", statusMessage);
    }
}
