package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import net.minecraft.core.Direction;

public class FollowOwnerGoal extends Goal {
    private final GolemEntity golem;
    private @Nullable Player owner;
    private int repath;

    public FollowOwnerGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private @Nullable Player findOwner() {
        if (golem.getOwner() == null) return null;
        Player p = golem.level().getPlayerByUUID(golem.getOwner());
        return p != null && !p.isSpectator() && p.isAlive() ? p : null;
    }

    @Override
    public boolean canUse() {
        if (golem.getJob() != Job.BLANK) return false;
        owner = findOwner();
        return owner != null && golem.distanceToSqr(owner) > 9.0;
    }

    @Override
    public boolean canContinueToUse() {
        return golem.getJob() == Job.BLANK && owner != null && owner.isAlive() && golem.distanceToSqr(owner) > 4.0;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        owner = null;
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        golem.getLookControl().setLookAt(owner, 10.0F, golem.getMaxHeadXRot());
        if (--repath > 0) return;
        repath = 10;
        if (golem.distanceToSqr(owner) > 400.0) {
            teleportNear(owner.blockPosition());
        } else {
            golem.getNavigation().moveTo(owner, 1.2);
        }
    }

    private void teleportNear(BlockPos pos) {
        for (int i = 0; i < 10; i++) {
            BlockPos p = pos.offset(golem.getRandom().nextInt(5) - 2, golem.getRandom().nextInt(3) - 1, golem.getRandom().nextInt(5) - 2);
            if (golem.level().getBlockState(p.below()).isFaceSturdy(golem.level(), p.below(), Direction.UP)
                    && golem.level().noCollision(golem, golem.getDimensions(golem.getPose()).makeBoundingBox(p.getBottomCenter()))) {
                golem.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, golem.getYRot(), golem.getXRot());
                golem.getNavigation().stop();
                return;
            }
        }
    }
}
