package com.workshopcraft.simplegolems.item;

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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class GolemItem extends Item {
    public GolemItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Job job, DyeColor colour) {
        ItemStack stack = new ItemStack(ModContent.GOLEM);
        stack.set(ModContent.JOB, job);
        if (colour != null) stack.set(ModContent.COLOUR, colour);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.simplegolems.golem." + stack.getOrDefault(ModContent.JOB, Job.BLANK).getSerializedName());
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
            var config = EntityType.<GolemEntity>createDefaultStackConfig(server, stack, context.getPlayer());
            GolemEntity golem = ModContent.GOLEM_ENTITY.create(server, config, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, false);
            if (golem == null) return InteractionResult.FAIL;
            golem.fromItem(stack);
            golem.setHome(pos);
            if (context.getPlayer() != null) golem.setOwner(context.getPlayer().getUUID());
            float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() + 180.0F : 0.0F;
            golem.snapTo(golem.getX(), golem.getY(), golem.getZ(), yaw, 0.0F);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        DyeColor colour = stack.get(ModContent.COLOUR);
        if (colour != null) out.accept(Component.translatable("color.minecraft." + colour.getName()).withStyle(ChatFormatting.GRAY));
        GolemSettings s = stack.get(ModContent.SETTINGS);
        if (s != null && s.area().isPresent()) {
            WorkArea a = s.area().get();
            out.accept(Component.translatable("tooltip.simplegolems.area", a.sizeX(), a.sizeY(), a.sizeZ()).withStyle(ChatFormatting.GRAY));
        }
    }
}
