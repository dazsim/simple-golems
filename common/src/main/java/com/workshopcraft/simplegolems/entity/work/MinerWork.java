package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// digs the marked box layer by layer from the top, leaving a staircase along the west edge so it can climb out
public class MinerWork implements JobWork {
    public static final MinerWork INSTANCE = new MinerWork();
    public static final int FLAT_DEPTH = 8;

    @Override
    public boolean wants(ItemEntity item) {
        return item.entityTags().contains(Storage.WORK_TAG);
    }

    @Override
    public WorkArea area(GolemEntity golem) {
        WorkArea a = golem.getArea();
        return a.sizeY() == 1 ? a.down(FLAT_DEPTH) : a;
    }

    @Override
    public boolean deliverFirst() {
        return true;
    }

    static boolean isStair(WorkArea area, BlockPos p) {
        int top = area.max().getY();
        return p.getX() == area.min().getX() && top - p.getY() > p.getZ() - area.min().getZ();
    }

    static boolean mineable(ServerLevel level, BlockPos p, ItemStack tool) {
        BlockState s = level.getBlockState(p);
        if (s.isAir() || !s.getFluidState().isEmpty() || s.hasBlockEntity() || s.getDestroySpeed(level, p) < 0) return false;
        if (s.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(s)) return false;
        if (level.getBlockEntity(p.above()) != null) return false;
        for (Direction d : Direction.values()) {
            if (d != Direction.DOWN && !level.getFluidState(p.relative(d)).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        ItemStack tool = golem.getTool();
        BlockPos min = area.min(), max = area.max();
        for (int y = max.getY(); y >= min.getY(); y--) {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(min.getX(), y, min.getZ(), max.getX(), y, max.getZ())) {
                if (isStair(area, p) || skip.test(p) || !mineable(level, p, tool)) continue;
                double d = golem.distanceToSqr(Vec3.atCenterOf(p));
                if (d < bestDist) {
                    best = p.immutable();
                    bestDist = d;
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).isAir();
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        ItemStack tool = golem.getTool();
        BlockState s = level.getBlockState(pos);
        float speed = Math.max(1.0F, tool.getDestroySpeed(s));
        return Mth.clamp(Math.round(s.getDestroySpeed(level, pos) * 30.0F / speed) + 4, 4, 80);
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        ItemStack tool = golem.getTool();
        if (tool.isEmpty() || !mineable(level, pos, tool)) return;
        BlockState s = level.getBlockState(pos);
        var drops = Block.getDrops(s, level, pos, null, golem, tool);
        LumberWork.breakQuietly(level, pos, s, true);
        tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
        Vec3 at = Vec3.atCenterOf(pos);
        for (ItemStack d : LumberWork.merge(drops)) Storage.collectOrDrop(golem, level, at, d);
    }
}
