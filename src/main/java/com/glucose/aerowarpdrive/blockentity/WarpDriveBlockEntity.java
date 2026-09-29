package com.glucose.aerowarpdrive.blockentity;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.block.WarpDriveBlock;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.joml.*;

import java.lang.Math;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_DRIVE_BLOCK_ENTITY;
import static com.glucose.aerowarpdrive.core.WarpDriveStates.*;

public class WarpDriveBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private final EnergyStorage energyStorage = createEnergyStorage();
    private int consumptionPerCharge;
    private int chargingTicksRemaining;
    private int cooldownTicksRemaining;
    private String statusMessage = "";
    private BlockPos target;
    public int time;
    public boolean animate;

    public WarpDriveBlockEntity(BlockPos pos, BlockState blockState) {
        super(WARP_DRIVE_BLOCK_ENTITY.get(), pos, blockState);
        time = (int) (Math.random() * 100000);
        animate = true;
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
        int ticks = Math.max((int) (distance / 50), 200);
        this.chargingTicksRemaining = ticks;
        this.consumptionPerCharge = (int) Math.ceil(energyCost / ticks);
        this.cooldownTicksRemaining = 0;
        getLevel().setBlock(getBlockPos(), getBlockState().setValue(WarpDriveBlock.STATUS, READY), WarpDriveBlock.UPDATE_CLIENTS);
        this.statusMessage = "Ready to charge.\nWarp Cost: " + (int) (energyCost + ticks) + " FE.\nPower to begin charging";
    }

    private void teleportSubLevel(ServerSubLevel subLevel) {
        Collection<SubLevel> attachedSublevels = SubLevelHelper.getConnectedChain(subLevel);
        HashMap<ServerSubLevel, Vector3d> subLevelOffsets = new HashMap<>();
        HashMap<Player, Vec3> playerOffsets = new HashMap<>();

        Vector3d startRootPos = new Vector3d(subLevel.logicalPose().position());
        for (SubLevel subLevel2 : attachedSublevels) {
            subLevelOffsets.put((ServerSubLevel) subLevel2, subLevel2.logicalPose().position().sub(startRootPos));
        }

        Vec3 targetPos = target.getCenter().add(0,(subLevel.boundingBox().size().y / 2) + 1,0);
        Vector3d targetPos3d = new Vector3d(targetPos.x, targetPos.y, targetPos.z);

        PhysicsPipeline pipeline = SubLevelContainer.getContainer(subLevel.getLevel()).physicsSystem().getPipeline();
        for (ServerSubLevel other : subLevelOffsets.keySet()) {
            pipeline.resetVelocity(other);
            pipeline.teleport(
                    other,
                    subLevelOffsets.get(other).add(targetPos3d),
                    other.logicalPose().orientation()
            );
        }

        if (startRootPos.distance(targetPos3d) > 100 || true) {
            for (UUID uuid : subLevel.getTrackingPlayers()) {
                Player player =  subLevel.getLevel().getPlayerByUUID(uuid);
                playerOffsets.put(player, player.position().subtract(startRootPos.x, startRootPos.y, startRootPos.z));
            }

            for (Player player : playerOffsets.keySet()) {
                Vec3 offset = playerOffsets.get(player).add(targetPos);
                player.setPos(offset.x, offset.y, offset.z);
                player.setDeltaMovement(Vec3.ZERO);
                AeronauticsWarpDrive.LOGGER.info("sent player to {} {} {}", offset.x, offset.y, offset.z);

                // ensure the player actually gets to the destination
                player.getServer().tell(new TickTask(1, () -> {
                    player.setPos(offset.x, offset.y, offset.z);
                    player.setDeltaMovement(Vec3.ZERO);
                }));
            }
        }
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
        if (Sable.HELPER.isInPlotGrid(level, pos)) {
            ++blockEntity.time;
            blockEntity.animate = true;
        } else
            blockEntity.animate = false;
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
