package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// Ex Deorum: takes blocks the sieve's mesh can sift from the first chest, sifts them and takes the results to the last one
public class SifterWork extends StationWork {
    public static final SifterWork INSTANCE = new SifterWork();

    @Override
    protected int output() {
        return 2;
    }

    private static @Nullable BlockEntity sieve(GolemEntity golem, ServerLevel level) {
        BlockPos p = point(golem, 1);
        ExDeorum.Hooks h = ExDeorum.hooks;
        if (p == null || h == null) return null;
        BlockEntity be = level.getBlockEntity(p);
        return be != null && h.isSieve(be) ? be : null;
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        ExDeorum.Hooks h = ExDeorum.hooks;
        BlockEntity sieve = sieve(golem, level);
        if (h == null || sieve == null) {
            golem.setSupplies("sieve");
            return null;
        }
        if (!h.hasMesh(sieve)) {
            golem.setSupplies("mesh");
            return null;
        }
        golem.setSupplies(null);
        if (!room(golem)) return null;
        Predicate<ItemStack> ok = s -> h.canSift(sieve, s);
        if (h.busy(sieve) || ok.test(input(golem, 0))) return sieve.getBlockPos();
        if (!input(golem, 0).isEmpty()) return null;
        BlockPos from = point(golem, 0);
        return sourceHas(level, from, ok) && !skip.test(from) ? from : null;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) != null;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        ExDeorum.Hooks h = ExDeorum.hooks;
        return pos.equals(point(golem, 0)) || h == null ? 10 : Math.max(4, h.siftTicks() + 1);
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        ExDeorum.Hooks h = ExDeorum.hooks;
        BlockEntity sieve = sieve(golem, level);
        if (h == null || sieve == null) return;
        if (pos.equals(point(golem, 0)) && !pos.equals(sieve.getBlockPos())) {
            take(golem, level, pos, 0, s -> h.canSift(sieve, s));
            return;
        }
        if (!h.busy(sieve)) {
            ItemStack in = input(golem, 0);
            if (!h.canSift(sieve, in)) return;
            h.fill(sieve, in.split(1));
            golem.carriedChanged();
        }
        h.sift(sieve, level);
        golem.playSound(SoundEvents.SAND_HIT, 0.6F, 1.0F);
        collect(golem, level, pos);
    }
}
