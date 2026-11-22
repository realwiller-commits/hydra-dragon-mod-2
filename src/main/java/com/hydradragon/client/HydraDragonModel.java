package com.hydradragon.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class HydraDragonModel extends EntityModel<HydraDragonRenderState> {
    private static final float[] NECK_YAW = {0.5f, 0.0f, -0.5f};
    private static final float NECK_PITCH = -0.35f;

    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart[] necks = new ModelPart[3];
    private final ModelPart[] heads = new ModelPart[3];
    private final ModelPart[] jaws = new ModelPart[3];

    public HydraDragonModel(ModelPart root) {
        super(root);
        this.wingLeft = root.getChild("wing_left");
        this.wingRight = root.getChild("wing_right");
        this.tail1 = root.getChild("tail_1");
        this.tail2 = this.tail1.getChild("tail_2");
        this.tail3 = this.tail2.getChild("tail_3");
        for (int i = 0; i < 3; i++) {
            this.necks[i] = root.getChild("neck_" + i);
            this.heads[i] = this.necks[i].getChild("head_" + i);
            this.jaws[i] = this.heads[i].getChild("jaw_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Body with a ridge of spikes down the back
        CubeListBuilder body = CubeListBuilder.create().texOffs(0, 0).addBox(-6, -5, -16, 12, 10, 32);
        for (int i = 0; i < 5; i++) {
            body.texOffs(0, 0).addBox(-1, -8, -13 + i * 6, 2, 3, 3);
        }
        root.addOrReplaceChild("body", body, PartPose.offset(0, 12, 0));

        // Wings
        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0, -1.5f, -1.5f, 38, 3, 3)
                        .texOffs(0, 0).addBox(0, -0.5f, 1.5f, 36, 1, 24),
                PartPose.offset(6, 8, -6));
        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-38, -1.5f, -1.5f, 38, 3, 3)
                        .texOffs(0, 0).addBox(-36, -0.5f, 1.5f, 36, 1, 24),
                PartPose.offset(-6, 8, -6));

        // Tail (three segments, each a child of the one before so it can sway)
        PartDefinition t1 = root.addOrReplaceChild("tail_1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3, 0, 8, 6, 16)
                        .texOffs(0, 0).addBox(-1, -6, 4, 2, 3, 6),
                PartPose.offset(0, 12, 15));
        PartDefinition t2 = t1.addOrReplaceChild("tail_2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3, -2.5f, 0, 6, 5, 16)
                        .texOffs(0, 0).addBox(-1, -5.5f, 4, 2, 3, 6),
                PartPose.offset(0, 0, 16));
        t2.addOrReplaceChild("tail_3",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2, -2, 0, 4, 4, 18)
                        .texOffs(0, 0).addBox(-0.5f, -2, 18, 1, 4, 8),
                PartPose.offset(0, 0, 16));

        // Three necks, each ending in a head with horns, eyes, teeth and a jaw
        float[] neckX = {-5, 0, 5};
        for (int i = 0; i < 3; i++) {
            PartDefinition neck = root.addOrReplaceChild("neck_" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -2.5f, -14, 5, 5, 14),
                    PartPose.offsetAndRotation(neckX[i], 8, -15, NECK_PITCH, NECK_YAW[i], 0));

            PartDefinition head = neck.addOrReplaceChild("head_" + i,
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-4, -3, -10, 8, 6, 10)
                            .texOffs(0, 0).addBox(-3, -2, -16, 6, 4, 6)
                            .texOffs(0, 0).addBox(-4.5f, -7, -3, 2, 5, 2)
                            .texOffs(0, 0).addBox(2.5f, -7, -3, 2, 5, 2)
                            .texOffs(112, 112).addBox(-4.6f, -2, -7, 0.6f, 1.5f, 2)
                            .texOffs(112, 112).addBox(4, -2, -7, 0.6f, 1.5f, 2),
                    PartPose.offset(0, 0, -14));

            CubeListBuilder jaw = CubeListBuilder.create().texOffs(0, 0).addBox(-3, 0, -15, 6, 2, 14);
            for (int z = -14; z <= -8; z += 3) {
                jaw.texOffs(96, 112).addBox(-3, -1.5f, z, 1, 1.5f, 1);
                jaw.texOffs(96, 112).addBox(2, -1.5f, z, 1, 1.5f, 1);
            }
            head.addOrReplaceChild("jaw_" + i, jaw, PartPose.offset(0, 3, -1));
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HydraDragonRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;

        float flap = Mth.sin(t * 0.3f) * 0.55f - 0.1f;
        this.wingLeft.zRot = flap;
        this.wingRight.zRot = -flap;

        this.tail1.yRot = Mth.sin(t * 0.08f) * 0.2f;
        this.tail2.yRot = Mth.sin(t * 0.08f - 0.8f) * 0.25f;
        this.tail3.yRot = Mth.sin(t * 0.08f - 1.6f) * 0.3f;

        for (int i = 0; i < 3; i++) {
            this.necks[i].yRot = NECK_YAW[i] + Mth.sin(t * 0.06f + i * 2.1f) * 0.1f;
            this.necks[i].xRot = NECK_PITCH + Mth.sin(t * 0.05f + i) * 0.06f;
            this.heads[i].xRot = 0.25f;
            this.jaws[i].xRot = 0.12f + (Mth.sin(t * 0.25f + i * 1.7f) * 0.5f + 0.5f) * 0.2f;
        }
    }
}
