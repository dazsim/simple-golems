package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// mines one clicked block each time it grows back, such as the cobblestone of a generator; nothing around it is touched
public class CobblegenWork extends StationWork {
    public static final CobblegenWork INSTANCE = new CobblegenWork();

    @Override
    protected int inputs() {
        return 0;
    }

    @Override
    protected int output() {
        return 1;
    }

    private static boolean ready(GolemEntity golem, ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return !s.isAir() && s.getFluidState().isEmpty() && !s.hasBlockEntity() && s.getDestroySpeed(level, p) >= 0
                && !s.is(ModTags.MINER_NEVER_DIG) && golem.canHarvest(s);
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        BlockPos p = point(golem, 0);
        if (p == null || !room(golem) || !ready(golem, level, p)) return null;
        return p;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).isAir();
    }

    @Override
    public boolean isValid(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return ready(golem, level, pos);
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return golem.digTicks(level.getBlockState(pos), pos);
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        ItemStack tool = golem.getTool();
        if (tool.isEmpty() || !ready(golem, level, pos)) return;
        BlockState s = level.getBlockState(pos);
        var drops = Block.getDrops(s, level, pos, null, golem, tool);
        LumberWork.breakQuietly(level, pos, s, true);
        tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
        for (ItemStack d : LumberWork.merge(drops)) addResult(golem, level, d, Vec3.atCenterOf(pos));
    }
}
