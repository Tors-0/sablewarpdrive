package com.glucose.aerowarpdrive.network;

import net.minecraft.world.phys.Vec3;

public record ParticlePacket(int effectId, Vec3 pos, Vec3 data1, Vec3 data2, int extraInt) {}
