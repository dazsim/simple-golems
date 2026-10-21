package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// Ex Deorum: puts a block from the first chest on the clicked spot and breaks it with its hammer, results to the last chest
public class HammerWork extends StationWork {
    public static final HammerWork INSTANCE = new HammerWork();

    @Override
    protected int output() {
        return 2;
    }

    private static Predicate<ItemStack> hammerable(GolemEntity golem) {
        ExDeorum.Hooks h = ExDeorum.hooks;
        ItemStack tool = golem.getTool();
        return s -> h != null && s.getItem() instanceof BlockItem && h.canHammer(tool, s);
    }

    private static boolean placed(GolemEntity golem, ServerLevel level, BlockPos spot) {
        BlockState s = level.getBlockState(spot);
        return !s.isAir() && hammerable(golem).test(new ItemStack(s.getBlock().asItem()));
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        BlockPos spot = point(golem, 1);
        if (spot == null || !room(golem)) return null;
        BlockState s = level.getBlockState(spot);
        if (placed(golem, level, spot)) return spot;
        boolean free = s.isAir() || s.canBeReplaced() && s.getFluidState().isEmpty();
        golem.setSupplies(free ? null : "spot");
        if (!free) return null;
        if (hammerable(golem).test(input(golem, 0))) return spot;
        if (!input(golem, 0).isEmpty()) return null;
        BlockPos from = point(golem, 0);
        return sourceHas(level, from, hammerable(golem)) && !skip.test(from) ? from : null;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return pos.equals(point(golem, 1)) && !s.isAir() ? golem.digTicks(s, pos) : 6;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        BlockPos spot = point(golem, 1);
        if (!pos.equals(spot)) {
            take(golem, level, pos, 0, hammerable(golem));
            return;
        }
        ItemStack tool = golem.getTool();
        if (placed(golem, level, spot)) {
            BlockState s = level.getBlockState(spot);
            var drops = Block.getDrops(s, level, spot, null, golem, tool);
            LumberWork.breakQuietly(level, spot, s, true);
            tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
            for (ItemStack d : LumberWork.merge(drops)) addResult(golem, level, d, Vec3.atCenterOf(spot));
            return;
        }
        ItemStack in = input(golem, 0);
        if (!hammerable(golem).test(in) || golem.getBoundingBox().intersects(new AABB(spot))) return;
        BlockState state = ((BlockItem) in.getItem()).getBlock().defaultBlockState();
        level.setBlockAndUpdate(spot, state);
        level.playSound(null, spot, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
        in.shrink(1);
        golem.carriedChanged();
    }
}
