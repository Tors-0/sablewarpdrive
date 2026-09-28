package com.glucose.aerowarpdrive.client.render;

import com.glucose.aerowarpdrive.blockentity.WarpDriveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Quaternionf;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.MODID;

public class WarpDriveBlockEntityRenderer implements BlockEntityRenderer<WarpDriveBlockEntity> {
    private static final ResourceLocation WARP_CRYSTAL_LOCATION = ResourceLocation.fromNamespaceAndPath(MODID,"textures/block/void_crystal.png");
    private static final RenderType RENDER_TYPE;
    private static final float SIN_45;
    private static final String GLASS = "glass";
    private final ModelPart cube;
    private final ModelPart glass;

    public WarpDriveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart modelpart = context.bakeLayer(ModelLayers.END_CRYSTAL);
        this.glass = modelpart.getChild("glass");
        this.cube = modelpart.getChild("cube");
    }

    // This method is called every frame in order to render the block entity. Parameters are:
    // - blockEntity:   The block entity instance being rendered. Uses the generic type passed to the super interface.
    // - partialTick:   The amount of time, in fractions of a tick (0.0 to 1.0), that has passed since the last tick.
    // - poseStack:     The pose stack to render to.
    // - bufferSource:  The buffer source to get vertex buffers from.
    // - packedLight:   The light value of the block entity.
    // - packedOverlay: The current overlay value of the block entity, usually OverlayTexture.NO_OVERLAY.
    @Override
    public void render(WarpDriveBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        float f1 = blockEntity.animate ? ((float)blockEntity.time + partialTicks) * 3.0F : 0;
        VertexConsumer vertexconsumer = buffer.getBuffer(RENDER_TYPE);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        int i = OverlayTexture.NO_OVERLAY;

        int cooldownTicksRemaining = blockEntity.getCooldownTicksRemaining();
        int color = kelvinToRgb(cooldownTicksRemaining * 5);
        int tempLight = cooldownTicksRemaining < 200 ? Math.max(3, (int) (cooldownTicksRemaining / 13f)) : 15;
        tempLight = LightTexture.pack(tempLight, tempLight);

        poseStack.mulPose(Axis.YP.rotationDegrees(f1));
        poseStack.mulPose((new Quaternionf()).setAngleAxis(1.0471976F, SIN_45, 0.0F, SIN_45));
        this.glass.render(poseStack, vertexconsumer, packedLight, i);
        float f2 = 0.875F;
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.mulPose((new Quaternionf()).setAngleAxis(1.0471976F, SIN_45, 0.0F, SIN_45));
        poseStack.mulPose(Axis.YP.rotationDegrees(f1));
        this.glass.render(poseStack, vertexconsumer, packedLight, i);
        poseStack.scale(0.875F, 0.875F, 0.875F);
        poseStack.mulPose((new Quaternionf()).setAngleAxis(1.0471976F, SIN_45, 0.0F, SIN_45));
        poseStack.mulPose(Axis.YP.rotationDegrees(f1));
        this.cube.render(poseStack, vertexconsumer, tempLight, i, color);
        poseStack.popPose();
        poseStack.popPose();
    }

    static {
        RENDER_TYPE = RenderType.entityCutoutNoCull(WARP_CRYSTAL_LOCATION);
        SIN_45 = (float)Math.sin(0.7853981633974483);
    }

    public static int kelvinToRgb(float kelvin) {
        int red, green, blue;
        if (kelvin < 1000) {
            float temp = (1000 - kelvin) / 1000 * 255;
            red = 255;
            green = (int) temp;
            blue = (int) temp;
        } else {
            kelvin /= 100;
            if (kelvin <= 66) {
                red = 255;
            } else {
                red = (int) (329.698727446 * Math.pow(kelvin - 60, -0.1332047592));
                red = Math.clamp(red, 0, 255);
            }

            if (kelvin <= 66) {
                green = (int) (99.4708025861 * Math.log(kelvin) - 161.1195681661);
            } else {
                green = (int) (288.1221695283 * Math.pow(kelvin - 60, -0.0755148492));
            }
            green = Math.clamp(green, 0, 255);

            if (kelvin >= 66) {
                blue = 255;
            } else if (kelvin <= 19 && kelvin >= 10) {
                blue = 0;
            } else {
                blue = (int) (138.5177312231 * Math.log(kelvin - 10) - 305.0447927307);
                blue = Math.clamp(blue, 0, 255);
            }
        }

        return FastColor.ARGB32.color(red, green, blue);
    }
}
