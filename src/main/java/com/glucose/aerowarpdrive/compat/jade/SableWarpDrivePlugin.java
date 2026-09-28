package com.glucose.aerowarpdrive.compat.jade;

import com.glucose.aerowarpdrive.block.WarpDriveBlock;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.MODID;

@WailaPlugin
public class SableWarpDrivePlugin implements IWailaPlugin {
    public static ResourceLocation WARP_DRIVE = ResourceLocation.fromNamespaceAndPath(MODID, "warp_drive_status");

    @Override
    public void register(IWailaCommonRegistration registration) {

    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(WarpDriveComponentProvider.INSTANCE, WarpDriveBlock.class);
        registration.markAsClientFeature(WARP_DRIVE);
    }
}
