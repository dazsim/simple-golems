package com.workshopcraft.simplegolems.item;

import com.workshopcraft.simplegolems.Mc;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
//? if >=1.21.3 {
import net.minecraft.world.entity.EntitySpawnReason;
//?} else {
/*import net.minecraft.world.entity.MobSpawnType;
*///?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.20.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
//? if >=1.21.5 {
import com.workshopcraft.simplegolems.SimpleGolems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
//?}
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

public class GolemItem extends Item {
    public GolemItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Job job, DyeColor colour) {
        ItemStack stack = new ItemStack(ModContent.GOLEM);
        ItemData.set(stack, ModContent.JOB, job);
        if (colour != null) ItemData.set(stack, ModContent.COLOUR, colour);
        return stack;
    }

    //? if >=1.21.5 {
    // a golem item made on 1.20.1 comes through the world upgrade with its data left in custom_data
    public static void migrate(ItemStack stack, HolderLookup.Provider registries) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CompoundTag root = data.copyTag();
        if (!(root.get(SimpleGolems.MOD_ID) instanceof CompoundTag old)) return;
        if (!ItemData.has(stack, ModContent.JOB)) {
            ItemData.read(old, "job", Job.CODEC).ifPresent(j -> ItemData.set(stack, ModContent.JOB, j));
            ItemData.read(old, "colour", DyeColor.CODEC).ifPresent(c -> ItemData.set(stack, ModContent.COLOUR, c));
            ItemData.read(old, "settings", GolemSettings.CODEC).ifPresent(s -> ItemData.set(stack, ModContent.SETTINGS, s));
            List<ItemStack> items = new ArrayList<>();
            for (Tag t : old.getListOrEmpty("items")) items.add(ItemData.parseStack(t, registries));
            if (!items.isEmpty()) ItemData.setItems(stack, items);
        }
        root.remove(SimpleGolems.MOD_ID);
        if (root.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        migrate(stack, level.registryAccess());
    }
    //?}

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.simplegolems.golem." + ItemData.getOrDefault(stack, ModContent.JOB, Job.BLANK).getSerializedName());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() == Direction.DOWN) return InteractionResult.FAIL;
        Level level = context.getLevel();
        BlockPos pos = new BlockPlaceContext(context).getClickedPos();
        ItemStack stack = context.getItemInHand();
        Vec3 at = Vec3.atBottomCenterOf(pos);
        AABB box = ModContent.GOLEM_ENTITY.getDimensions().makeBoundingBox(at.x(), at.y(), at.z());
        if (!level.noCollision(null, box) || !level.getEntities(null, box).isEmpty()) return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            //? if >=1.21.3 {
            var config = EntityType.<GolemEntity>createDefaultStackConfig(server, stack, context.getPlayer());
            GolemEntity golem = ModContent.GOLEM_ENTITY.create(server, config, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, false);
            //?} else {
            /*GolemEntity golem = ModContent.GOLEM_ENTITY.create(server, null, null, pos, MobSpawnType.SPAWN_EGG, true, false);
            *///?}
            if (golem == null) return InteractionResult.FAIL;
            golem.fromItem(stack);
            golem.setHome(pos);
            if (context.getPlayer() != null) golem.setOwner(context.getPlayer().getUUID());
            float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() + 180.0F : 0.0F;
            //? if >=1.21.5 {
            Mc.place(golem, golem.getX(), golem.getY(), golem.getZ(), yaw, 0.0F);
            //?} else {
            /*golem.moveTo(golem.getX(), golem.getY(), golem.getZ(), yaw, 0.0F);
            *///?}
            golem.setYHeadRot(yaw);
            golem.setYBodyRot(yaw);
            server.addFreshEntity(golem);
            level.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.DECORATED_POT_PLACE, SoundSource.NEUTRAL, 0.8F, 1.3F);
            golem.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    //? if >=1.21.5 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, @org.jspecify.annotations.Nullable Level level, List<Component> lines, TooltipFlag flag) {
        Consumer<Component> out = lines::add;
    *///?}
        DyeColor colour = ItemData.get(stack, ModContent.COLOUR);
        if (colour != null) out.accept(Component.translatable("color.minecraft." + colour.getName()).withStyle(ChatFormatting.GRAY));
        GolemSettings s = ItemData.get(stack, ModContent.SETTINGS);
        if (s != null && s.area().isPresent()) {
            WorkArea a = s.area().get();
            out.accept(Component.translatable("tooltip.simplegolems.area", a.sizeX(), a.sizeY(), a.sizeZ()).withStyle(ChatFormatting.GRAY));
        }
    }
}
