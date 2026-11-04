package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.work.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

// a golem whose tool broke walks to storage in its area and takes the best matching tool there
public class FetchToolGoal extends Goal {
    private static final BlockState[] TIERS = {Blocks.OBSIDIAN.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState(),
            Blocks.IRON_ORE.defaultBlockState(), Blocks.STONE.defaultBlockState()};

    private final GolemEntity golem;
    private @Nullable BlockPos chest;
    private int cooldown;
    private int ticks;

    public FetchToolGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean wanted() {
        Job job = golem.getJob();
        return job.needsTool() && golem.getTool().isEmpty() && golem.isPowered();
    }

    @Override
    public boolean canUse() {
        if (golem.level().isClientSide() || !wanted()) return false;
        if (cooldown-- > 0) return false;
        cooldown = 40;
        chest = Storage.nearest((ServerLevel) golem.level(), golem.getArea(), golem.position(), c -> best(c) >= 0, p -> false);
        return chest != null;
    }

    @Override
    public boolean canContinueToUse() {
        return chest != null && wanted() && ticks < 400;
    }

    @Override
    public void start() {
        ticks = 0;
        golem.getNavigation().moveTo(chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        ticks++;
        golem.getLookControl().setLookAt(Vec3.atCenterOf(chest));
        if (golem.distanceToSqr(Vec3.atCenterOf(chest)) > 6.25) {
            if (golem.getNavigation().isDone() && ticks % 20 == 0) start();
            return;
        }
        Container c = Storage.containerAt((ServerLevel) golem.level(), chest);
        int slot = c == null ? -1 : best(c);
        if (slot >= 0) {
            golem.setTool(c.removeItem(slot, 1));
            c.setChanged();
            golem.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 0.8F, 1.2F);
        }
        chest = null;
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
        chest = null;
    }

    // highest tier first: what it can harvest, then durability, which follows the material tier (gold is lowest)
    private int best(Container c) {
        int slot = -1;
        float bestScore = -1;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty() || !golem.getJob().usesTool(s)) continue;
            float score = 0;
            for (BlockState t : TIERS) score = score * 2 + (s.isCorrectToolForDrops(t) ? 1 : 0);
            score = score * 10000 + Math.min(9999, s.getMaxDamage());
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }
        return slot;
    }
}
