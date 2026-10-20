package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.work.LumberWork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

// a lumber golem that ended up on a tree breaks its way down: it fells the tree it stands on, or clears the leaves under it
public class GetOffTreeGoal extends Goal {
    private final GolemEntity golem;
    private @Nullable BlockPos target;
    private boolean fell;
    private int ticks;
    private int need;

    public GetOffTreeGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.level().isClientSide() || golem.getJob() != Job.LUMBER || !golem.onGround() || golem.isWaiting()
                || golem.tickCount % 10 != 0 || !(golem.level() instanceof ServerLevel level)) return false;
        BlockPos floor = golem.getOnPos();
        BlockPos base = LumberWork.baseUnder(level, floor);
        if (base != null && golem.canHarvest(level.getBlockState(base))) {
            target = base;
            fell = true;
            need = LumberWork.INSTANCE.workTicks(golem, level, base);
        } else if (LumberWork.isTreeLeaf(level, floor)) {
            target = floor;
            fell = false;
            need = golem.digTicks(level.getBlockState(floor), floor);
        } else {
            return false;
        }
        ticks = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && !golem.isWaiting() && !golem.level().getBlockState(target).isAir();
    }

    @Override
    public void start() {
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ServerLevel level = (ServerLevel) golem.level();
        if (!golem.usePower(1)) return;
        if (ticks % 10 == 0) golem.swing(InteractionHand.MAIN_HAND);
        if (++ticks < need) return;
        if (fell) LumberWork.INSTANCE.finish(golem, level, target);
        else LumberWork.breakLeaf(golem, level, target);
        target = null;
    }

    @Override
    public void stop() {
        target = null;
    }
}
