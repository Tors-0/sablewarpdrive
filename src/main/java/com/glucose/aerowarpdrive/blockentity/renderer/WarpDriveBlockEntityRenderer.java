package com.glucose.aerowarpdrive.blockentity.renderer;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.block.WarpDriveBlock;
import com.glucose.aerowarpdrive.blockentity.WarpDriveBlockEntity;
import com.glucose.aerowarpdrive.client.render.ModModelLayers;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class WarpDriveBlockEntityRenderer implements BlockEntityRenderer<WarpDriveBlockEntity> {
    private static final ResourceLocation WARP_CRYSTAL_LOCATION = AeronauticsWarpDrive.id("textures/block/void_crystal.png");
    private static final ResourceLocation WARP_DRIVE_FIELD_LOCATION = AeronauticsWarpDrive.id("textures/block/warp_drive_field.png");
    private static final RenderType RENDER_TYPE_MAIN = RenderType.entityCutoutNoCull(WARP_CRYSTAL_LOCATION);
    private static final RenderType RENDER_TYPE_FIELD = RenderType.entityTranslucentEmissive(WARP_DRIVE_FIELD_LOCATION);
    private static final float SIN_45 = (float)Math.sin(0.7853981633974483);
    private static final String GLASS = "glass";
    private final ModelPart cube;
    private final ModelPart glass;
    private final ModelPart field;

    public WarpDriveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart modelpart = context.bakeLayer(ModelLayers.END_CRYSTAL);
        this.glass = modelpart.getChild("glass");
        this.cube = modelpart.getChild("cube");
        ModelPart field = context.bakeLayer(ModModelLayers.WARP_DRIVE_FIELD_LAYER);
        this.field = field.getChild("root");
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
        if (blockEntity.getBlockState().getValue(WarpDriveBlock.ASSEMBLED)) {
            poseStack.pushPose();
            poseStack.pushPose();
            int cooldownTicksRemaining = blockEntity.getCooldownTicksRemaining();
            blockEntity.time += cooldownTicksRemaining / 200f;
            float f1 = blockEntity.animate ? ((blockEntity.time + partialTicks) * 3.0F) : 0;

            Vec3 translation = switch (blockEntity.getFacingForMultiblock()) {
                case NORTH -> new Vec3(0.5F, 2.5F, 2.5F);
                case EAST -> new Vec3(-1.5F, 2.5F, 0.5F);
                case SOUTH -> new Vec3(0.5F, 2.5F, -1.5F);
                case WEST -> new Vec3(2.5F, 2.5F, 0.5F);
                default -> new Vec3(0, 0, 0); // this should never happen
            };

            poseStack.translate(translation.x, translation.y, translation.z);
            renderWarpDriveCore(cooldownTicksRemaining, poseStack, buffer, f1);
            poseStack.popPose();

            poseStack.translate(translation.x, translation.y, translation.z);
            poseStack.scale(1 + (float) (Math.sin(f1 / 10) * 0.001), 1, 1 + (float) (Math.cos(f1 / 10) * 0.001));
            this.field.render(poseStack, buffer.getBuffer(RENDER_TYPE_FIELD), LightTexture.pack(15, 15), packedOverlay);
            poseStack.popPose();
        }
    }

    private void renderWarpDriveCore(int cooldownTicksRemaining, PoseStack poseStack, MultiBufferSource buffer, float f1) {
        VertexConsumer vertexconsumer = buffer.getBuffer(RENDER_TYPE_MAIN);
        int i = OverlayTexture.NO_OVERLAY;

        int color = kelvinToRgb(cooldownTicksRemaining * 5);
        int tempLight = cooldownTicksRemaining < 200 ? Math.max(3, (int) (cooldownTicksRemaining / 13f)) : 15;
        tempLight = LightTexture.pack(tempLight, tempLight);
        int maxLight = LightTexture.pack(15,15);
        var scale = Math.clamp(1 + (float) cooldownTicksRemaining / 400, 1f, 3f);
        poseStack.scale(scale, scale, scale);

//        poseStack.scale(4,4,4);
        poseStack.mulPose(Axis.YP.rotationDegrees(f1));
        poseStack.mulPose((new Quaternionf()).setAngleAxis(1.0471976F, SIN_45, 0.0F, SIN_45));
        this.glass.render(poseStack, vertexconsumer, maxLight, i);
        float f2 = 0.75F;
        poseStack.scale(f2, f2, f2);
        poseStack.mulPose((new Quaternionf()).setAngleAxis(1.0471976F, SIN_45, 0.0F, SIN_45));
        poseStack.mulPose(Axis.YP.rotationDegrees(f1));
        this.glass.render(poseStack, vertexconsumer, maxLight, i);
//        poseStack.scale(1f/3, 1f/3, 1f/3);

        poseStack.scale(f2, f2, f2);
        poseStack.mulPose((new Quaternionf()).setAngleAxis(1.0471976F, SIN_45, 0.0F, SIN_45));
        poseStack.mulPose(Axis.YP.rotationDegrees(f1));
        this.cube.render(poseStack, vertexconsumer, tempLight, i, color);
    }

    public static int kelvinToRgb(float kelvin) {
        int red, green, blue;
        if (kelvin > 3000) kelvin = 3000;
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
