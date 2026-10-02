package com.glucose.aerowarpdrive.util;

import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;

// lawfully copied code from https://github.com/rearth/Oritech as per the terms of the CC0 1.0 Universal license
public interface MachineControllerLifecycle extends IBlockEntityExtension {
    @Override
    default void onLoad() {
        IBlockEntityExtension.super.onLoad();
        onControllerLoad((BlockEntity) this);
    }

    default void onControllerLoad(BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) return;

        var server = serverLevel.getServer();
        server.tell(new TickTask(server.getTickCount(), () -> {
            if (blockEntity.isRemoved() || blockEntity.getLevel() != serverLevel) return;

            if (this instanceof MultiblockMachineController multiblockController) {
                multiblockController.rescanMultiblock();
            }

            blockEntity.setChanged();
        }));
    }
}