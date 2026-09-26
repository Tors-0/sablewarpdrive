package com.glucose.aerowarpdrive;

import com.glucose.aerowarpdrive.blockentity.WarpDriveBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_DRIVE;
import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_DRIVE_BLOCK_ENTITY;

@EventBusSubscriber(modid = AeronauticsWarpDrive.MODID)
public class AeronauticsWarpCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
//        event.registerBlock(
//                Capabilities.EnergyStorage.BLOCK,
//                ((level, blockPos, blockState, blockEntity, direction) -> blockEntity instanceof WarpDriveBlockEntity warpDrive ? warpDrive.getEnergyStorage() : null),
//                WARP_DRIVE.get()
//        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                WARP_DRIVE_BLOCK_ENTITY.get(),
                WarpDriveBlockEntity::getEnergyStorage
        );
    }
}
