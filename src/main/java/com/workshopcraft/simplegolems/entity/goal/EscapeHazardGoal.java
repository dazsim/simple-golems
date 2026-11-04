package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

// in lava, fire or burning: head for the nearest safe dry spot
public class EscapeHazardGoal extends Goal {
    private final GolemEntity golem;
    private @Nullable BlockPos safe;

    public EscapeHazardGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    private boolean inDanger() {
        return golem.isInLava() || golem.isOnFire() || golem.level().getBlockState(golem.blockPosition()).is(BlockTags.FIRE);
    }

    @Override
    public boolean canUse() {
        if (!inDanger()) return false;
        safe = findSafe(golem.level(), golem.blockPosition());
        return safe != null;
    }

    @Override
    public boolean canContinueToUse() {
        return safe != null && inDanger() && !golem.getNavigation().isDone();
    }

    @Override
    public void start() {
        golem.getNavigation().moveTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, 1.4);
    }

    private static @Nullable BlockPos findSafe(Level level, BlockPos from) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-6, -2, -6), from.offset(6, 3, 6))) {
            if (!clear(level, p) || !clear(level, p.above())) continue;
            if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) continue;
            if (!level.getFluidState(p.below()).isEmpty()) continue;
            double d = p.distSqr(from);
            if (d < bestDist) {
                best = p.immutable();
                bestDist = d;
            }
        }
        return best;
    }

    private static boolean clear(Level level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getFluidState(p).isEmpty() && !level.getBlockState(p).is(BlockTags.FIRE);
    }
}
