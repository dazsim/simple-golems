package com.workshopcraft.simplegolems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.Trouble;
import net.minecraft.util.Mth;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
//? if >=1.21.2 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
*///?}

//? if >=1.21.2 {
public class GolemRenderer extends MobRenderer<GolemEntity, GolemRenderState, GolemModel> {
//?} else {
/*public class GolemRenderer extends MobRenderer<GolemEntity, GolemModel> {
*///?}
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleGolems.id("golem"), "main");
    private static final Identifier TINT = SimpleGolems.id("textures/entity/golem/golem_tint.png");
    private static final Identifier OVERLAY = SimpleGolems.id("textures/entity/golem/golem_overlay.png");
    private static final Identifier GLOW = SimpleGolems.id("textures/entity/golem/golem_glow.png");
    private static final float TILT = 17.0F * Mth.DEG_TO_RAD;

    //? if >=1.21.2 {
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
        state.attackTime = golem./*? if >=26.3 {*//*getSwingAnimation*//*?} else {*/getAttackAnim/*?}*/(partialTicks);
        state.job = golem.getJob();
        state.colour = tint(golem.getColour());
        state.powered = golem.isPowered();
        state.waiting = golem.isWaiting();
        state.downed = golem.isDowned();
        itemModelResolver.updateForLiving(state.carried, golem.getShownCarried(), ItemDisplayContext.GROUND, golem);
        state.carrying = !state.carried.isEmpty();
        state.trouble = golem.shownTrouble;
        state.bubble = Mth.lerp(partialTicks, golem.bubbleO, golem.bubble);
        float t = Mth.lerp(partialTicks, golem.tiltO, golem.tilt);
        // curious head tilt, each golem leans its own way
        state.headTilt = t * t * (3 - 2 * t) * TILT * ((golem.getId() & 1) == 0 ? 1 : -1);
    }

    @Override
    public void submit(GolemRenderState state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        super.submit(state, pose, out, camera);
        if (state.bubble <= 0.0F || state.trouble == Trouble.NONE) return;
        pose.pushPose();
        pose.translate(0.0F, state.boundingBoxHeight + 0.42F + Mth.sin(state.ageInTicks * 0.08F) * 0.04F, 0.0F);
        //? if >=26.3 {
        /*pose.rotate(camera.orientation);
        *///?} else
        pose.mulPose(camera.orientation);
        float a = state.bubble * state.bubble * (3 - 2 * state.bubble);
        pose.scale(0.75F, 0.75F, 0.75F);
        // the trailing dots sit in the lower left of the texture, so shift the cloud up and right of the head
        pose.translate(0.32F, 0.3F, 0.0F);
        int alpha = (int) (a * 255);
        out.submitCustomGeometry(pose, RenderTypes.entityTranslucent(bubbleTexture(state.trouble, state.job)), (p, vc) -> {
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

    //?} else {
    /*private final ItemRenderer items;
    private final GolemModel cardModel, glowModel;

    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(LAYER), GolemModel.Pass.BODY), 0.4F);
        items = context.getItemRenderer();
        cardModel = new GolemModel(context.bakeLayer(LAYER), GolemModel.Pass.CARD);
        glowModel = new GolemModel(context.bakeLayer(LAYER), GolemModel.Pass.BODY);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, GolemEntity golem, float limb, float limbSpeed, float partial, float age, float yRot, float xRot) {
                Job job = golem.getJob();
                //? if >=1.21 {
                renderColoredCutoutModel(getParentModel(), OVERLAY, pose, buffers, light, golem, -1);
                if (job != Job.BLANK) {
                    coloredCutoutModelCopyLayerRender(getParentModel(), cardModel, OVERLAY, pose, buffers, light, golem, limb, limbSpeed, age, yRot, xRot, partial,
                            0xFF000000 | job.colour);
                }
                //?} else {
                /^renderColoredCutoutModel(getParentModel(), OVERLAY, pose, buffers, light, golem, 1.0F, 1.0F, 1.0F);
                if (job != Job.BLANK) {
                    coloredCutoutModelCopyLayerRender(getParentModel(), cardModel, OVERLAY, pose, buffers, light, golem, limb, limbSpeed, age, yRot, xRot, partial,
                            (job.colour >> 16 & 255) / 255.0F, (job.colour >> 8 & 255) / 255.0F, (job.colour & 255) / 255.0F);
                }
                ^///?}
            }
        });
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, GolemEntity golem, float limb, float limbSpeed, float partial, float age, float yRot, float xRot) {
                if (golem.isInvisible()) return;
                getParentModel().copyPropertiesTo(glowModel);
                glowModel.prepareMobModel(golem, limb, limbSpeed, partial);
                glowModel.setupAnim(golem, limb, limbSpeed, age, yRot, xRot);
                float f = golem.isPowered() ? 1.0F : 0.3F;
                //? if >=1.21 {
                int g = (int) (f * 255);
                glowModel.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFF000000 | g << 16 | g << 8 | g);
                //?} else {
                /^glowModel.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, f, f, f, 1.0F);
                ^///?}
            }
        });
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, GolemEntity golem, float limb, float limbSpeed, float partial, float age, float yRot, float xRot) {
                held(golem, golem.getTool(), true, pose, buffers, light);
                held(golem, golem.getShownCarried(), false, pose, buffers, light);
            }
        });
    }

    private void held(GolemEntity golem, ItemStack stack, boolean tool, PoseStack pose, MultiBufferSource buffers, int light) {
        if (stack.isEmpty()) return;
        pose.pushPose();
        if (tool) getModel().poseTool(pose);
        else getModel().poseCarried(pose);
        items.renderStatic(golem, stack, tool ? ItemDisplayContext.FIXED : ItemDisplayContext.GROUND, false, pose, buffers, golem.level(), light, OverlayTexture.NO_OVERLAY, golem.getId());
        pose.popPose();
    }

    @Override
    public Identifier getTextureLocation(GolemEntity golem) {
        return TINT;
    }

    @Override
    public void render(GolemEntity golem, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = Mth.lerp(partial, golem.tiltO, golem.tilt);
        // curious head tilt, each golem leans its own way
        getModel().headTilt = t * t * (3 - 2 * t) * TILT * ((golem.getId() & 1) == 0 ? 1 : -1);
        getModel().tint = 0xFF000000 | tint(golem.getColour());
        super.render(golem, yaw, partial, pose, buffers, light);
        float bubble = Mth.lerp(partial, golem.bubbleO, golem.bubble);
        if (bubble <= 0.0F || golem.shownTrouble == Trouble.NONE) return;
        pose.pushPose();
        pose.translate(0.0F, golem.getBbHeight() + 0.42F + Mth.sin((golem.tickCount + partial) * 0.08F) * 0.04F, 0.0F);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float a = bubble * bubble * (3 - 2 * bubble);
        pose.scale(0.75F, 0.75F, 0.75F);
        pose.translate(0.32F, 0.3F, 0.0F);
        int alpha = (int) (a * 255);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(bubbleTexture(golem.shownTrouble, golem.getJob())));
        quad(pose.last(), vc, alpha, false);
        quad(pose.last(), vc, alpha, true);
        pose.popPose();
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, int alpha, boolean back) {
        float[][] v = {{-0.5F, -0.5F, 0, 1}, {0.5F, -0.5F, 1, 1}, {0.5F, 0.5F, 1, 0}, {-0.5F, 0.5F, 0, 0}};
        for (int i = 0; i < 4; i++) {
            float[] c = v[back ? 3 - i : i];
            //? if >=1.21 {
            vc.addVertex(p, c[0], c[1], 0.0F).setColor(255, 255, 255, alpha).setUv(c[2], c[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(p, 0.0F, 0.0F, back ? -1.0F : 1.0F);
            //?} else {
            /^vc.vertex(p.pose(), c[0], c[1], 0.0F).color(255, 255, 255, alpha).uv(c[2], c[3])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(p.normal(), 0.0F, 0.0F, back ? -1.0F : 1.0F).endVertex();
            ^///?}
        }
    }
    *///?}

    private static final int FULL_BRIGHT = 0xF000F0;

    private static Identifier bubbleTexture(Trouble trouble, Job job) {
        String name = switch (trouble) {
            case STORAGE -> "storage";
            case SAPLING -> "sapling";
            case FOOD -> "food";
            case THANKS -> "thanks";
            case BLOCKS -> "blocks";
            case SUPPLIES -> "supplies";
            case HURT -> "hurt";
            default -> switch (job) {
                case FARMER -> "hoe";
                case MINER, COBBLEGEN -> "pickaxe";
                case SHEPHERD, BEEKEEPER -> "shears";
                case FISHER -> "rod";
                case HAMMER -> "hammer";
                case GUARD -> "sword";
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
