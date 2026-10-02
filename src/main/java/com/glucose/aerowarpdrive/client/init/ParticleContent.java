package com.glucose.aerowarpdrive.client.init;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.network.ParticlePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// lawfully copied code from https://github.com/rearth/Oritech as per the terms of the CC0 1.0 Universal license
public class ParticleContent {

    public enum EffectType {
        HIGHLIGHT_BLOCK,
    }

    // public stuff

    public static void HighlightBlock(Level world, Vec3 pos) {
        sendParticle(world, new ParticlePacket(EffectType.HIGHLIGHT_BLOCK.ordinal(), pos, Vec3.ZERO, Vec3.ZERO, 0));
    }

    private static void sendParticle(Level world, ParticlePacket payload) {
        if (world instanceof ServerLevel sl) {
            double rSq = 64 * 64;
            for (var player : sl.players()) {
                if (player.distanceToSqr(payload.pos()) < rSq) {
                    AeronauticsWarpDrive.PARTICLES_CHANNEL.serverHandle(player).send(payload);
                }
            }
        } else if (world.isClientSide) {
            handleOnClient(payload, world);
        }
    }

    // client handler

    public static void handleOnClient(ParticlePacket payload, Level world) {
        var type = EffectType.values()[payload.effectId()];
        switch (type) {
            case HIGHLIGHT_BLOCK -> spawnCubeOutline(ParticleTypes.ELECTRIC_SPARK, payload.pos(), 1, 120, 6);
        }
    }

    // client utilities

    private static void spawnCubeOutline(ParticleOptions particle, Vec3 origin, float size, int duration, int segments) {
        spawnLineWithAge(particle, origin, origin.add(size, 0, 0), segments, duration);
        spawnLineWithAge(particle, origin.add(size, 0, 0), origin.add(size, 0, size), segments, duration);
        spawnLineWithAge(particle, origin, origin.add(0, 0, size), segments, duration);
        spawnLineWithAge(particle, origin.add(0, 0, size), origin.add(size, 0, size), segments, duration);

        origin = origin.add(0, size, 0);

        spawnLineWithAge(particle, origin, origin.add(size, 0, 0), segments, duration);
        spawnLineWithAge(particle, origin.add(size, 0, 0), origin.add(size, 0, size), segments, duration);
        spawnLineWithAge(particle, origin, origin.add(0, 0, size), segments, duration);
        spawnLineWithAge(particle, origin.add(0, 0, size), origin.add(size, 0, size), segments, duration);

        spawnLineWithAge(particle, origin, origin.add(0, -size, 0), segments, duration);
        spawnLineWithAge(particle, origin.add(size, 0, 0), origin.add(size, -size, 0), segments, duration);
        spawnLineWithAge(particle, origin.add(0, 0, size), origin.add(0, -size, size), segments, duration);
        spawnLineWithAge(particle, origin.add(size, 0, size), origin.add(size, -size, size), segments, duration);
    }

    private static void spawnLineWithAge(ParticleOptions particle, Vec3 start, Vec3 end, float count, int maxAge) {
        var mc = Minecraft.getInstance();
        Vec3 step = end.subtract(start).scale(1f / count);
        for (int i = 0; i < count; i++) {
            var p = mc.particleEngine.createParticle(particle, start.x, start.y, start.z, 0, 0, 0);
            if (p != null) p.setLifetime(maxAge);
            start = start.add(step);
        }
    }
}