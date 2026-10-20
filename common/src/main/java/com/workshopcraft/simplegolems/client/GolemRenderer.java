package com.workshopcraft.simplegolems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeColor;

public class GolemRenderer extends MobRenderer<GolemEntity, GolemRenderState, GolemModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleGolems.id("golem"), "main");
    private static final Identifier TINT = SimpleGolems.id("textures/entity/golem/golem_tint.png");
    private static final Identifier OVERLAY = SimpleGolems.id("textures/entity/golem/golem_overlay.png");
    private static final Identifier GLOW = SimpleGolems.id("textures/entity/golem/golem_glow.png");

    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(LAYER), GolemModel.Pass.BODY), 0.4F);
        GolemModel cardModel = new GolemModel(context.bakeLayer(LAYER), GolemModel.Pass.CARD);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector out, int light, GolemRenderState state, float yRot, float xRot) {
                coloredCutoutModelCopyLayerRender(getParentModel(), OVERLAY, pose, out, light, state, -1, 1);
                if (state.job != Job.BLANK) {
                    coloredCutoutModelCopyLayerRender(cardModel, OVERLAY, pose, out, light, state, ARGB.opaque(state.job.colour), 1);
                }
            }
        });
        addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> s.powered ? 1.0F : 0.3F,
                new GolemModel(context.bakeLayer(LAYER), GolemModel.Pass.BODY), RenderTypes::eyes, false));
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector out, int light, GolemRenderState state, float yRot, float xRot) {
                if (state.tool.isEmpty()) return;
                pose.pushPose();
                getParentModel().poseTool(pose);
                state.tool.submit(pose, out, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                pose.popPose();
            }
        });
    }

    @Override
    public Identifier getTextureLocation(GolemRenderState state) {
        return TINT;
    }

    @Override
    protected int getModelTint(GolemRenderState state) {
        return ARGB.opaque(state.colour);
    }

    @Override
    public GolemRenderState createRenderState() {
        return new GolemRenderState();
    }

    @Override
    public void extractRenderState(GolemEntity golem, GolemRenderState state, float partialTicks) {
        super.extractRenderState(golem, state, partialTicks);
        itemModelResolver.updateForLiving(state.tool, golem.getTool(), ItemDisplayContext.FIXED, golem);
        state.attackTime = golem.getAttackAnim(partialTicks);
        state.job = golem.getJob();
        state.colour = tint(golem.getColour());
        state.powered = golem.isPowered();
        state.waiting = golem.isWaiting();
        state.carrying = golem.isCarrying();
    }

    // terracotta averages lifted a little so dark dyes stay friendly, from the art colour sheet
    private static final int UNDYED = 0xBE7858;
    private static final int[] DYES = {0xFFDEC9, 0xC96A33, 0xBA7088, 0x8F88AC, 0xE7A631, 0x839445, 0xC96464, 0x4B3931,
            0xAA867B, 0x6E7474, 0x955B6E, 0x5F4E74, 0x634431, 0x626A39, 0xB3503E, 0x332019};

    public static int tint(DyeColor colour) {
        return colour == null ? UNDYED : DYES[colour.getId()];
    }
}
