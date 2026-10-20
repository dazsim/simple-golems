package com.workshopcraft.simplegolems.client;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// holding clay cookies and looking at a golem shows how full its cookie tin is on the action bar
public final class CookieHud {
    private static final double RANGE = 5.0;

    private CookieHud() {
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || player.tickCount % 4 != 0) return;
        if (!holdsCookies(player)) return;
        GolemEntity golem = lookedAt(mc, player);
        if (golem == null) return;
        int max = new ItemStack(ModContent.CLAY_COOKIE).getMaxStackSize();
        mc.gui.setOverlayMessage(Component.translatable("hud.simplegolems.cookies", golem.getShownCookies(), max), false);
    }

    private static boolean holdsCookies(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(ModContent.CLAY_COOKIE)) return true;
        }
        return false;
    }

    private static GolemEntity lookedAt(Minecraft mc, LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(RANGE));
        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box, e -> e instanceof GolemEntity && !e.isSpectator(), RANGE * RANGE);
        if (hit == null) return null;
        // a block in the way hides the golem
        HitResult block = mc.hitResult;
        double d = eye.distanceToSqr(hit.getLocation());
        if (block != null && block.getType() == HitResult.Type.BLOCK && eye.distanceToSqr(block.getLocation()) < d) return null;
        return (GolemEntity) hit.getEntity();
    }
}
