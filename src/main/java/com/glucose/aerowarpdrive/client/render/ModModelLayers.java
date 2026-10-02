package com.glucose.aerowarpdrive.client.render;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ModModelLayers {
    public static final ModelLayerLocation WARP_DRIVE_FIELD_LAYER = new ModelLayerLocation(AeronauticsWarpDrive.id("warp_drive_field"), "field");

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partDefinition = mesh.getRoot();

        PartDefinition root = partDefinition.addOrReplaceChild("root", CubeListBuilder.create().texOffs(0, -64).addBox(-32.0F, -48.0F, -32.0F, 0.0F, 48.0F, 64.0F, new CubeDeformation(0.0F))
                .texOffs(-32, 0).addBox(-24.0F, 8.0F, -24.0F, 48.0F, 0.0F, 48.0F, new CubeDeformation(0.0F))
                .texOffs(-32, 0).addBox(-24.0F, -56.0F, -24.0F, 48.0F, 0.0F, 48.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition cube_n_r1 = root.addOrReplaceChild("cube_n_r1", CubeListBuilder.create().texOffs(0, -64).addBox(-32.0F, -48.0F, -48.0F, 0.0F, 48.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition cube_s_r1 = root.addOrReplaceChild("cube_s_r1", CubeListBuilder.create().texOffs(0, -64).addBox(0.0F, -48.0F, -48.0F, 0.0F, 48.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, 0.0F, 32.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition cube_w_r1 = root.addOrReplaceChild("cube_w_r1", CubeListBuilder.create().texOffs(0, -64).addBox(0.0F, -48.0F, -32.0F, 0.0F, 48.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(32.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

        LayerDefinition def = LayerDefinition.create(mesh, 128, 48);
        event.registerLayerDefinition(WARP_DRIVE_FIELD_LAYER, () -> def);
    }
}
