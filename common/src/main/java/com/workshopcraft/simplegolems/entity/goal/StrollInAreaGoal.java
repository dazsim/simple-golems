package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public class StrollInAreaGoal extends Goal {
    private final GolemEntity golem;
    private @Nullable Vec3 target;

    public StrollInAreaGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (golem.getJob() == Job.BLANK || golem.isWaiting() || golem.isFollowing()) return false;
        WorkArea area = golem.getArea();
        if (!area.contains(golem.blockPosition())) {
            BlockPos min = area.min(), max = area.max();
            BlockPos p = golem.blockPosition();
            target = golem.getCustomArea() == null ? Vec3.atBottomCenterOf(golem.getHome())
                    : Vec3.atBottomCenterOf(new BlockPos(inside(p.getX(), min.getX(), max.getX()), Math.clamp(p.getY(), min.getY(), max.getY()), inside(p.getZ(), min.getZ(), max.getZ())));
            return true;
        }
        if (golem.getRandom().nextInt(120) != 0) return false;
        for (int i = 0; i < 5; i++) {
            Vec3 v = DefaultRandomPos.getPos(golem, 6, 3);
            if (v != null && area.contains(BlockPos.containing(v))) {
                target = v;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !golem.getNavigation().isDone() && !golem.isWaiting();
    }

    @Override
    public void start() {
        if (target != null) golem.getNavigation().moveTo(target.x, target.y, target.z, 0.8);
    }

    // a block in from the edge, so a golem just outside walks properly back in
    private static int inside(int v, int min, int max) {
        return max - min >= 2 ? Math.clamp(v, min + 1, max - 1) : Math.clamp(v, min, max);
    }
}
