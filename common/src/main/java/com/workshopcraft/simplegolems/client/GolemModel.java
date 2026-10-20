package com.workshopcraft.simplegolems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplegolems.entity.Job;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GolemModel extends EntityModel<GolemRenderState> {
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
    private final Map<Job, List<ModelPart>> accents = new EnumMap<>(Job.class);
    private final List<ModelPart> all;

    public GolemModel(ModelPart root, Pass pass) {
        super(root);
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
        ModelPart lumber = head.getChild("acc_lumber");
        accents.put(Job.LUMBER, List.of(lumber, lumber.getChild("acc_lumber_leaf_l"), lumber.getChild("acc_lumber_leaf_r")));
        accents.put(Job.FARMER, List.of(head.getChild("acc_farmer")));
        accents.put(Job.MINER, List.of(head.getChild("acc_miner")));
        accents.put(Job.COURIER, List.of(body.getChild("acc_courier")));
        accents.put(Job.SWEEPER, List.of(body.getChild("acc_sweeper")));
        all = root.getAllParts();
    }

    @Override
    public void setupAnim(GolemRenderState state) {
        super.setupAnim(state);
        for (Map.Entry<Job, List<ModelPart>> e : accents.entrySet()) {
            for (ModelPart p : e.getValue()) p.visible = e.getKey() == state.job;
        }
        card.visible = state.job != Job.BLANK;
        for (ModelPart p : all) p.skipDraw = pass == Pass.CARD;
        card.skipDraw = pass != Pass.CARD;

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        legL.xRot = -1.4F * Mth.triangleWave(pos, 8.0F) * speed;
        legR.xRot = 1.4F * Mth.triangleWave(pos, 8.0F) * speed;
        toolArm.loadPose(toolArmPose);
        cardArm.loadPose(cardArmPose);
        toolArm.xRot += 0.6F * Mth.triangleWave(pos, 8.0F) * speed;
        cardArm.xRot -= 0.8F * Mth.triangleWave(pos, 8.0F) * speed;
        if (state.carrying) cardArm.xRot = -0.9F;
        if (state.attackTime > 0.0F) {
            float swing = Mth.sin(Mth.sqrt(state.attackTime) * Mth.PI);
            toolArm.xRot = toolArmPose.xRot() - 1.6F * swing;
        }
        if (state.waiting) {
            head.xRot = 0.35F;
            toolArm.xRot = toolArmPose.xRot() * 0.3F;
        }
    }

    public void poseTool(PoseStack pose) {
        root().translateAndRotate(pose);
        body.translateAndRotate(pose);
        toolArm.translateAndRotate(pose);
        pose.translate(0.0F, 6.4F / 16.0F, -0.8F / 16.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(TOOL_TILT));
        pose.scale(TOOL_SCALE, TOOL_SCALE, TOOL_SCALE);
        pose.translate(-0.38F, 0.38F, 0.0F);
    }

    private static final float TOOL_TILT = 105.0F;
    private static final float TOOL_SCALE = 0.62F;
}
