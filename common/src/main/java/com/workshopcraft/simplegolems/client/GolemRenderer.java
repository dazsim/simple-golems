package com.workshopcraft.simplegolems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.Trouble;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
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
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector out, int light, GolemRenderState state, float yRot, float xRot) {
                if (state.carried.isEmpty()) return;
                pose.pushPose();
                getParentModel().poseCarried(pose);
                state.carried.submit(pose, out, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
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
        itemModelResolver.updateForLiving(state.carried, golem.getShownCarried(), ItemDisplayContext.GROUND, golem);
        state.carrying = !state.carried.isEmpty();
        state.trouble = golem.shownTrouble;
        state.bubble = Mth.lerp(partialTicks, golem.bubbleO, golem.bubble);
    }

    @Override
    public void submit(GolemRenderState state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        super.submit(state, pose, out, camera);
        if (state.bubble <= 0.0F || state.trouble == Trouble.NONE) return;
        pose.pushPose();
        pose.translate(0.0F, state.boundingBoxHeight + 0.42F + Mth.sin(state.ageInTicks * 0.08F) * 0.04F, 0.0F);
        pose.mulPose(camera.orientation);
        float a = state.bubble * state.bubble * (3 - 2 * state.bubble);
        pose.scale(0.75F, 0.75F, 0.75F);
        // the trailing dots sit in the lower left of the texture, so shift the cloud up and right of the head
        pose.translate(0.32F, 0.3F, 0.0F);
        int alpha = (int) (a * 255);
        out.submitCustomGeometry(pose, RenderTypes.entityTranslucent(bubbleTexture(state)), (p, vc) -> {
            quad(p, vc, alpha, false);
            quad(p, vc, alpha, true);
        });
        pose.popPose();
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, int alpha, boolean back) {
        float[][] v = {{-0.5F, -0.5F, 0, 1}, {0.5F, -0.5F, 1, 1}, {0.5F, 0.5F, 1, 0}, {-0.5F, 0.5F, 0, 0}};
        for (int i = 0; i < 4; i++) {
            float[] c = v[back ? 3 - i : i];
            vc.addVertex(p, c[0], c[1], 0.0F).setColor(255, 255, 255, alpha).setUv(c[2], c[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(p, 0.0F, 0.0F, back ? -1.0F : 1.0F);
        }
    }

    private static final int FULL_BRIGHT = 0xF000F0;

    private static Identifier bubbleTexture(GolemRenderState state) {
        String name = switch (state.trouble) {
            case STORAGE -> "storage";
            case SAPLING -> "sapling";
            case FOOD -> "food";
            case THANKS -> "thanks";
            case BLOCKS -> "blocks";
            case SUPPLIES -> "supplies";
            default -> switch (state.job) {
                case FARMER -> "hoe";
                case MINER, COBBLEGEN -> "pickaxe";
                case SHEPHERD, BEEKEEPER -> "shears";
                case FISHER -> "rod";
                case HAMMER -> "hammer";
                default -> "axe";
            };
        };
        return SimpleGolems.id("textures/entity/golem/bubble_" + name + ".png");
    }

    // terracotta averages lifted a little so dark dyes stay friendly, from the art colour sheet
    private static final int UNDYED = 0xBE7858;
    private static final int[] DYES = {0xFFDEC9, 0xC96A33, 0xBA7088, 0x8F88AC, 0xE7A631, 0x839445, 0xC96464, 0x4B3931,
            0xAA867B, 0x6E7474, 0x955B6E, 0x5F4E74, 0x634431, 0x626A39, 0xB3503E, 0x332019};

    public static int tint(DyeColor colour) {
        return colour == null ? UNDYED : DYES[colour.getId()];
    }
}
