package com.glucose.aerowarpdrive.compat.jade;

import com.glucose.aerowarpdrive.block.WarpDriveBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum WarpDriveComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        iTooltip.add(Component.translatable("string.aerowarpdrive.status").append(blockAccessor.getBlockState().getValue(WarpDriveBlock.STATUS).toString()));
    }

    @Override
    public ResourceLocation getUid() {
        return SableWarpDrivePlugin.WARP_DRIVE;
    }
}
