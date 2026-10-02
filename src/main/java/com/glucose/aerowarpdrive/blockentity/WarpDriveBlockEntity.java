package com.glucose.aerowarpdrive.blockentity;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.block.WarpDriveBlock;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.util.MultiblockMachineController;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.joml.*;

import java.lang.Math;
import java.util.*;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_DRIVE_BLOCK_ENTITY;
import static com.glucose.aerowarpdrive.block.WarpDriveBlock.ASSEMBLED;
import static com.glucose.aerowarpdrive.block.WarpDriveBlock.FACING;
import static com.glucose.aerowarpdrive.core.WarpDriveStates.*;

public class WarpDriveBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, MultiblockMachineController {
    private final EnergyStorage energyStorage = createEnergyStorage();
    private static final EnergyStorage nilStorage = new EnergyStorage(0);
    private int consumptionPerCharge;
    private int chargingTicksRemaining;
    private int cooldownTicksRemaining;
    private String statusMessage = "";
    private BlockPos target;
    public float time;
    public boolean animate;
    private final ArrayList<BlockPos> coresConnected = new ArrayList<>();

    public WarpDriveBlockEntity(BlockPos pos, BlockState blockState) {
        super(WARP_DRIVE_BLOCK_ENTITY.get(), pos, blockState);
        time = (int) (Math.random() * 100000);
        animate = true;
    }

    private EnergyStorage createEnergyStorage() {
        return new EnergyStorage(100_000);
    }

    public EnergyStorage getEnergyStorage(Direction side) {
        if (side == null) return energyStorage;
        if (side.equals(getBlockState().getValue(FACING))) return nilStorage;
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
        int ticks = Math.max((int) (distance / 10), 200);
        this.chargingTicksRemaining = ticks;
        this.consumptionPerCharge = (int) Math.ceil(energyCost / ticks);
        this.cooldownTicksRemaining = 0;
        getLevel().setBlock(getBlockPos(), getBlockState().setValue(WarpDriveBlock.STATUS, READY), WarpDriveBlock.UPDATE_CLIENTS);
        this.statusMessage = "Ready to charge.\nWarp Cost: " + (int) (energyCost + ticks) + " FE.\nPower to begin charging";
    }

    private void teleportSubLevel(ServerSubLevel subLevel) {
        Collection<SubLevel> attachedSublevels = SubLevelHelper.getConnectedChain(subLevel);
        HashMap<ServerSubLevel, Vector3d> subLevelOffsets = new HashMap<>();

        Vector3d startRootPos = new Vector3d(subLevel.logicalPose().position());
        for (SubLevel subLevel2 : attachedSublevels) {
            subLevelOffsets.put((ServerSubLevel) subLevel2, subLevel2.logicalPose().position().sub(startRootPos));
        }

        Vec3 targetPos = target.getCenter().add(0,(subLevel.boundingBox().size().y / 2) + 1,0);
        Vector3d targetPos3d = new Vector3d(targetPos.x, targetPos.y, targetPos.z);

        // collect all entities within the bounding box of the sublevel
        Map<Entity, Vec3> onboardEntities = new HashMap<>();

        SubLevelPhysicsSystem physSystem = SubLevelContainer.getContainer(subLevel.getLevel()).physicsSystem();
        for (ServerSubLevel other : subLevelOffsets.keySet()) {
            // get onboard entities
            getLevel().getEntities(null, other.boundingBox().toMojang().inflate(1.0)).forEach(entity -> {
                onboardEntities.put(entity, entity.position().subtract(startRootPos.x, startRootPos.y, startRootPos.z));
            });

            physSystem.getPhysicsHandle(other).teleport(
                    subLevelOffsets.get(other).add(targetPos3d),
                    other.logicalPose().orientation()
            );
            other.updateLastPose();
        }

        // put onboard entities back on the sublevel after move
        onboardEntities.forEach(((entity, offset) -> {

            Vec3 finalPosition = offset.add(targetPos);
            entity.setPos(finalPosition.x, finalPosition.y, finalPosition.z);
            entity.setDeltaMovement(Vec3.ZERO);
            AeronauticsWarpDrive.LOGGER.info("Sent entity {} to {}", entity.getName(), finalPosition);

            // ensure the entity actually gets to the destination
            // sable is cool af but hell to work with
//            Objects.requireNonNull(entity.getServer()).tell(new TickTask(subLevel.getLevel().getServer().getTickCount() + 1, () -> {
//                entity.teleportTo(finalPosition.x, finalPosition.y, finalPosition.z);
//                entity.setDeltaMovement(Vec3.ZERO);
//                AeronauticsWarpDrive.LOGGER.info("Re-Sent entity {} to {}", entity.getName(), finalPosition);
//            }));
        }));
    }

    public int getCooldownTicksRemaining() {
        return cooldownTicksRemaining;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WarpDriveBlockEntity blockEntity) {
        switch (state.getValue(WarpDriveBlock.STATUS)) {
            case CHARGING -> blockEntity.cooldownTicksRemaining+=2;
            case COOLING -> blockEntity.cooldownTicksRemaining--;
            default -> blockEntity.cooldownTicksRemaining = 0;
        }
        if (Sable.HELPER.isInPlotGrid(level, pos) && level.getBlockState(pos).getValue(ASSEMBLED)) {
            ++blockEntity.time;
            blockEntity.animate = true;
        } else {
            blockEntity.animate = false;
            blockEntity.time = 0;
        }
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        switch (getBlockState().getValue(WarpDriveBlock.STATUS)) {
            case READY -> {
                // destination selected, consuming power
                if (getLevel().hasNeighborSignal(getBlockPos())) {
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
                        this.statusMessage = "Charge failed, cooling down";
                    } else {
                        this.chargingTicksRemaining--;
                        this.cooldownTicksRemaining += 2;
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
        loadMultiblockNbtData(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy_stored",energyStorage.serializeNBT(registries));
        tag.putInt("consumptionPerCharge", consumptionPerCharge);
        tag.putInt("chargingTicks", chargingTicksRemaining);
        tag.putInt("cooldownTicks", cooldownTicksRemaining);
        tag.putString("statusMessage", statusMessage);
        addMultiblockToNbt(tag);
    }

    private static final List<Vec3i> corePositions = List.of(
            new Vec3i(0,0,2), // near side low
            new Vec3i(0,0,1),
            new Vec3i(0,0,-1),
            new Vec3i(0,0,-2),
            new Vec3i(4,0,-2), // far side low
            new Vec3i(4,0,-1),
            new Vec3i(4,0,0),
            new Vec3i(4,0,1),
            new Vec3i(4,0,2),
            new Vec3i(1,0,2), // right low
            new Vec3i(2,0,2),
            new Vec3i(3,0,2),
            new Vec3i(1,0,-2), // left low
            new Vec3i(2,0,-2),
            new Vec3i(3,0,-2),
            new Vec3i(0,4,2), // near side high
            new Vec3i(0,4,1),
            new Vec3i(0,4,0),
            new Vec3i(0,4,-1),
            new Vec3i(0,4,-2),
            new Vec3i(4,4,-2), // far side hi
            new Vec3i(4,4,-1),
            new Vec3i(4,4,0),
            new Vec3i(4,4,1),
            new Vec3i(4,4,2),
            new Vec3i(1,4,2), // right hi
            new Vec3i(2,4,2),
            new Vec3i(3,4,2),
            new Vec3i(1,4,-2), // left hi
            new Vec3i(2,4,-2),
            new Vec3i(3,4,-2)
    );

    @Override
    public List<Vec3i> getCorePositions() {
        return corePositions;
    }

    @Override
    public Direction getFacingForMultiblock() {
        return this.getBlockState().getValue(FACING);
    }

    @Override
    public BlockPos getPosForMultiblock() {
        return this.getBlockPos();
    }

    @Override
    public Level getWorldForMultiblock() {
        return this.getLevel();
    }

    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coresConnected;
    }

    @Override
    public EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return this.energyStorage;
    }
}
