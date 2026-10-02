package com.glucose.aerowarpdrive.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class ModRenderTypes {
    public static RenderType cutoutNoCullEmissive() {
        return RenderType.create("custom_basic", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> ModShaders.BASIC_SHADER)) // here to use your shader program
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                        .setOverlayState(RenderStateShard.NO_OVERLAY)
                        .createCompositeState(false));
    }
}
