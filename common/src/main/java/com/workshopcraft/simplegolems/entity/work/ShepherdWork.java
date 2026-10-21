package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Predicate;

// shears grown sheep in its area; the wool goes to storage
public class ShepherdWork implements JobWork {
    public static final ShepherdWork INSTANCE = new ShepherdWork();

    @Override
    public boolean wants(ItemEntity item) {
        return item.getItem().is(ItemTags.WOOL);
    }

    private static @Nullable Sheep near(ServerLevel level, AABB box, BlockPos from) {
        return level.getEntitiesOfClass(Sheep.class, box, s -> s.isAlive() && s.readyForShearing()).stream()
                .min(Comparator.comparingDouble(s -> s.distanceToSqr(from.getCenter()))).orElse(null);
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        Sheep s = level.getEntitiesOfClass(Sheep.class, area.box(), e -> e.isAlive() && e.readyForShearing() && !skip.test(e.blockPosition())).stream()
                .min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        return s == null ? null : s.blockPosition();
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return near(level, new AABB(pos).inflate(2.5), pos) != null;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 10;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        Sheep s = near(level, golem.getBoundingBox().inflate(3.0), golem.blockPosition());
        ItemStack tool = golem.getTool();
        if (s == null || tool.isEmpty()) return;
        s.shear(level, SoundSource.NEUTRAL, tool);
        tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
    }
}
