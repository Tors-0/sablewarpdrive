package com.glucose.aerowarpdrive.core;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum WarpDriveStates implements StringRepresentable {
    COOLING,
    WAITING,
    CHARGING,
    READY;

    @Override
    @NotNull
    public String getSerializedName() {
        return switch (this) {
            case COOLING -> "cooling";
            case WAITING -> "waiting";
            case CHARGING -> "charging";
            case READY -> "ready";
        };
    }
}
