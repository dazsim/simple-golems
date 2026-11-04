package com.workshopcraft.simplegolems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplegolems.entity.Job;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

//? if <1.21.2 {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import com.workshopcraft.simplegolems.entity.GolemEntity;
*///?}

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

//? if >=1.21.2 {
public class GolemModel extends EntityModel<GolemRenderState> {
//?} else {
/*public class GolemModel extends EntityModel<GolemEntity> {
*///?}
    public enum Pass { BODY, CARD }

    private final Pass pass;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart toolArm;
    private final ModelPart cardArm;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart card;
    private final PartPose toolArmPose;
    private final PartPose cardArmPose;
    private final float toolArmX, toolArmZ, cardArmZ;
    private final ModelPart rootPart;
    private final Map<Job, List<ModelPart>> accents = new EnumMap<>(Job.class);
    private final List<ModelPart> all;

    public GolemModel(ModelPart root, Pass pass) {
        //? if >=1.21.2 {
        super(root);
        //?}
        rootPart = root;
        this.pass = pass;
        body = root.getChild("body");
        head = body.getChild("head");
        // the art names arms as seen from the front, so its left arm is the golem's right (tool) hand
        toolArm = body.getChild("arm_left");
        cardArm = body.getChild("arm_right");
        legL = root.getChild("leg_left");
        legR = root.getChild("leg_right");
        card = cardArm.getChild("card");
        toolArmPose = toolArm.storePose();
        cardArmPose = cardArm.storePose();
        toolArmX = toolArm.xRot;
        toolArmZ = toolArm.zRot;
        cardArmZ = cardArm.zRot;
        ModelPart lumber = head.getChild("acc_lumber");
        accents.put(Job.LUMBER, List.of(lumber, lumber.getChild("acc_lumber_leaf_l"), lumber.getChild("acc_lumber_leaf_r")));
        accents.put(Job.FARMER, List.of(head.getChild("acc_farmer")));
        accents.put(Job.MINER, List.of(head.getChild("acc_miner")));
        accents.put(Job.COURIER, List.of(body.getChild("acc_courier")));
        accents.put(Job.SWEEPER, List.of(body.getChild("acc_sweeper")));
        //? if >=1.21.2 {
        all = root.getAllParts();
        //?} else {
        /*all = root.getAllParts().toList();
        *///?}
    }

    //? if >=1.21.2 {
    @Override
    public void setupAnim(GolemRenderState state) {
        super.setupAnim(state);
        pose(state.job, state.walkAnimationPos, state.walkAnimationSpeed, state.yRot, state.xRot, state.headTilt,
                state.carrying, state.attackTime, state.waiting, state.downed);
    }
    //?} else {
    /*public float headTilt;
    public int tint = -1;

    @Override
    public void setupAnim(GolemEntity golem, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        pose(golem.getJob(), pos, speed, yRot, xRot, headTilt, !golem.getShownCarried().isEmpty(), attackTime, golem.isWaiting(), golem.isDowned());
    }

    @Override
    public void copyPropertiesTo(EntityModel<GolemEntity> other) {
        super.copyPropertiesTo(other);
        if (other instanceof GolemModel g) g.headTilt = headTilt;
    }

    // the body tint is used once, by the main pass, so layers drawn with this model after it are not tinted
    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        float tr = (tint >> 16 & 255) / 255.0F, tg = (tint >> 8 & 255) / 255.0F, tb = (tint & 255) / 255.0F;
        tint = -1;
        rootPart.render(pose, vc, light, overlay, r * tr, g * tg, b * tb, a);
    }
    *///?}

    private void pose(Job job, float pos, float walkSpeed, float yRot, float xRot, float tilt, boolean carrying, float attackTime, boolean waiting, boolean downed) {
        //? if <1.21.2 {
        /*for (ModelPart p : all) p.resetPose();
        rootPart.y = 24.0F;
        *///?}
        for (Map.Entry<Job, List<ModelPart>> e : accents.entrySet()) {
            for (ModelPart p : e.getValue()) p.visible = e.getKey() == job;
        }
        card.visible = job != Job.BLANK;
        for (ModelPart p : all) p.skipDraw = pass == Pass.CARD;
        card.skipDraw = pass != Pass.CARD;

        float speed = Math.min(walkSpeed, 1.0F);
        head.yRot = yRot * Mth.DEG_TO_RAD;
        head.xRot = xRot * Mth.DEG_TO_RAD * 0.5F;
        head.zRot = tilt;
        legL.xRot = -1.4F * Mth.triangleWave(pos, 8.0F) * speed;
        legR.xRot = 1.4F * Mth.triangleWave(pos, 8.0F) * speed;
        toolArm.loadPose(toolArmPose);
        cardArm.loadPose(cardArmPose);
        toolArm.xRot += 0.6F * Mth.triangleWave(pos, 8.0F) * speed;
        cardArm.xRot -= 0.8F * Mth.triangleWave(pos, 8.0F) * speed;
        if (carrying) cardArm.xRot = -1.1F;
        if (attackTime > 0.0F) {
            float swing = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            toolArm.xRot = toolArmX - 1.6F * swing;
        }
        if (waiting) {
            head.xRot = 0.55F;
            toolArm.xRot = toolArmX * 0.3F;
            cardArm.zRot = cardArmZ * 0.5F;
        }
        if (downed) {
            body.y += 2.0F;
            body.xRot = 0.25F;
            head.xRot = 0.6F;
            for (ModelPart leg : List.of(legL, legR)) {
                leg.xRot = -Mth.HALF_PI;
                leg.y += 0.5F;
            }
            toolArm.xRot = -0.25F;
            toolArm.zRot = toolArmZ + 0.35F;
            cardArm.xRot = -0.25F;
            cardArm.zRot = cardArmZ - 0.35F;
        }
    }

    public void poseTool(PoseStack pose) {
        rootPart.translateAndRotate(pose);
        body.translateAndRotate(pose);
        toolArm.translateAndRotate(pose);
        pose.translate(0.0F, 6.4F / 16.0F, -0.8F / 16.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(TOOL_TILT));
        pose.scale(TOOL_SCALE, TOOL_SCALE, TOOL_SCALE);
        pose.translate(-0.38F, 0.38F, 0.0F);
    }

    public void poseCarried(PoseStack pose) {
        rootPart.translateAndRotate(pose);
        body.translateAndRotate(pose);
        cardArm.translateAndRotate(pose);
        pose.translate(0.0F, 7.0F / 16.0F, -1.0F / 16.0F);
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.scale(0.75F, 0.75F, 0.75F);
    }

    private static final float TOOL_TILT = 105.0F;
    private static final float TOOL_SCALE = 0.62F;
}
