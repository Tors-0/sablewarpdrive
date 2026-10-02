package com.glucose.aerowarpdrive.client.render;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

@EventBusSubscriber
public class ModShaders {
    private static final Logger LOGGER = LoggerFactory.getLogger("aerowarpdrive/ModShaders");
    public static ShaderInstance BASIC_SHADER;

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        register(event, "basic", DefaultVertexFormat.POSITION, shader -> BASIC_SHADER = shader); // don't modify the "basic" name.
    }

    /**
     * 1. reading "<modid>/shaders/core/*.json"
     * 2. parse the "*.json" file, read 'samplers' and 'uniforms'
     * 3. compile vertex and fragment program
     * 4. linking program
     * 5. cache uniform location
     * x. use 'apply()' to apply shader.
     * @param name the "*.json" file actually name.
     * @param format vertex format
     * @param consumer lambda statement
     */
    private static void register(RegisterShadersEvent event, String name, VertexFormat format, Consumer<ShaderInstance> consumer) {
        try {
            ResourceProvider provider = event.getResourceProvider();
            ShaderInstance instance = new ShaderInstance(provider, AeronauticsWarpDrive.id(name), format);
            event.registerShader(instance, consumer);
        } catch (IOException e) {
            LOGGER.warn("failed to load shader: {}", name);
            LOGGER.debug("{}", e.toString());
        }
    }
}
