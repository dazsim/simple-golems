package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

// holds the golem still while it has no power, no tool or nowhere to put what it carries
public class WaitGoal extends Goal {
    private final GolemEntity golem;

    public WaitGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return golem.isWaiting() || golem.hasStorageTrouble();
    }

    @Override
    public void start() {
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        golem.getNavigation().stop();
    }
}
