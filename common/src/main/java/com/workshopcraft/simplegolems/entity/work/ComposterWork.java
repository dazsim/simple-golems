package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// fills a composter from the first chest and takes the bone meal to the last one
public class ComposterWork extends StationWork {
    public static final ComposterWork INSTANCE = new ComposterWork();
    private static final Predicate<ItemStack> COMPOSTABLE = s -> ComposterBlock.COMPOSTABLES.containsKey(s.getItem());

    @Override
    protected int output() {
        return 2;
    }

    private static int fill(ServerLevel level, @Nullable BlockPos p) {
        if (p == null) return -1;
        BlockState s = level.getBlockState(p);
        return s.getBlock() instanceof ComposterBlock ? s.getValue(ComposterBlock.LEVEL) : -1;
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        BlockPos bin = point(golem, 1);
        int fill = fill(level, bin);
        golem.setSupplies(fill < 0 ? "composter" : null);
        if (fill < 0 || !room(golem)) return null;
        if (fill == ComposterBlock.READY) return bin;
        if (fill >= ComposterBlock.MAX_LEVEL) return null;
        if (COMPOSTABLE.test(input(golem, 0))) return bin;
        if (!input(golem, 0).isEmpty()) return null;
        BlockPos from = point(golem, 0);
        return sourceHas(level, from, COMPOSTABLE) && !skip.test(from) ? from : null;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 8;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        BlockPos bin = point(golem, 1);
        if (!pos.equals(bin)) {
            take(golem, level, pos, 0, COMPOSTABLE);
            return;
        }
        BlockState s = level.getBlockState(bin);
        int fill = fill(level, bin);
        if (fill == ComposterBlock.READY) {
            level.setBlock(bin, s.setValue(ComposterBlock.LEVEL, 0), 3);
            level.playSound(null, bin, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            addResult(golem, level, new ItemStack(Items.BONE_MEAL), Vec3.atCenterOf(bin).add(0, 0.6, 0));
            return;
        }
        // a few at a time, the way a player clicks
        for (int i = 0; i < 4 && fill >= 0 && fill < ComposterBlock.MAX_LEVEL && COMPOSTABLE.test(input(golem, 0)); i++) {
            BlockState now = ComposterBlock.insertItem(golem, level.getBlockState(bin), level, input(golem, 0), bin);
            level.levelEvent(1500, bin, now != s ? 1 : 0);
            s = now;
            fill = fill(level, bin);
        }
        golem.carriedChanged();
    }
}
