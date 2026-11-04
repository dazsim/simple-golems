package com.workshopcraft.simplegolems;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if >=1.21.9 {
import net.minecraft.world.level.gamerules.GameRules;
//?} else {
/*import net.minecraft.world.level.GameRules;
*///?}

// small calls that differ between the Minecraft versions this builds for
public final class Mc {
    private Mc() {}

    public static void place(Entity e, double x, double y, double z, float yRot, float xRot) {
        //? if >=1.21.5 {
        e.snapTo(x, y, z, yRot, xRot);
        //?} else {
        /*e.moveTo(x, y, z, yRot, xRot);
        *///?}
    }

    public static int minY(Level level) {
        //? if >=1.21.2 {
        return level.getMinY();
        //?} else {
        /*return level.getMinBuildHeight();
        *///?}
    }

    public static BlockPos minPos(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    public static BlockPos maxPos(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public static Set<String> tags(Entity e) {
        //? if >=1.21.9 {
        return e.entityTags();
        //?} else {
        /*return e.getTags();
        *///?}
    }

    public static boolean sameItem(ItemStack a, ItemStack b) {
        //? if >=1.20.5 {
        return ItemStack.isSameItemSameComponents(a, b);
        //?} else {
        /*return ItemStack.isSameItemSameTags(a, b);
        *///?}
    }

    // one point of wear on a tool in the main hand
    public static void wear(ItemStack tool, LivingEntity user) {
        //? if >=1.20.5 {
        tool.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
        //?} else {
        /*tool.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        *///?}
    }

    public static boolean mobGriefing(ServerLevel level) {
        //? if >=1.21.9 {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
        //?} else {
        /*return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        *///?}
    }

    public static SoundEvent eatSound() {
        //? if >=1.20.5 {
        return SoundEvents.GENERIC_EAT.value();
        //?} else {
        /*return SoundEvents.GENERIC_EAT;
        *///?}
    }

    public static SoundEvent equipSound() {
        //? if >=1.20.5 {
        return SoundEvents.ARMOR_EQUIP_GENERIC.value();
        //?} else {
        /*return SoundEvents.ARMOR_EQUIP_GENERIC;
        *///?}
    }

    public static SoundEvent potInsert() {
        //? if >=1.20.4 {
        return SoundEvents.DECORATED_POT_INSERT;
        //?} else {
        /*return SoundEvents.DECORATED_POT_HIT;
        *///?}
    }

    public static SoundEvent lead(boolean tied) {
        //? if >=1.21.5 {
        return tied ? SoundEvents.LEAD_TIED : SoundEvents.LEAD_UNTIED;
        //?} else {
        /*return tied ? SoundEvents.LEASH_KNOT_PLACE : SoundEvents.LEASH_KNOT_BREAK;
        *///?}
    }

    public static boolean creative(Player player) {
        //? if >=1.21.2 {
        return player.hasInfiniteMaterials();
        //?} else {
        /*return player.getAbilities().instabuild;
        *///?}
    }

    public static void overlay(Player player, Component message) {
        //? if >=1.21.9 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }

    public static void consume(ItemStack stack, int n, Player player) {
        //? if >=1.21 {
        stack.consume(n, player);
        //?} else {
        /*if (!creative(player)) stack.shrink(n);
        *///?}
    }
}
