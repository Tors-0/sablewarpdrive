package com.glucose.aerowarpdrive.network;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public record WarpDriveTargetPacket(UUID anchorId, BlockPos pos) {}
