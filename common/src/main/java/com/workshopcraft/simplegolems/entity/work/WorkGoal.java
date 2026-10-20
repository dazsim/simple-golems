package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkGoal extends Goal {
    private enum Task { PICKUP, DELIVER, WORK }

    private static final int GIVE_UP = 400;
    private static final int SKIP_FOR = 1200;

    private final GolemEntity golem;
    private final Map<BlockPos, Long> skipped = new HashMap<>();
    private @Nullable Task task;
    private @Nullable JobWork work;
    private @Nullable BlockPos pos;
    private @Nullable ItemEntity item;
    private int ticks;
    private int workTicks;
    private int cooldown;
    private boolean failed;

    public WorkGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private ServerLevel level() {
        return (ServerLevel) golem.level();
    }

    private boolean skip(BlockPos p) {
        Long until = skipped.get(p);
        return until != null && until > golem.level().getGameTime();
    }

    @Override
    public boolean canUse() {
        if (golem.level().isClientSide() || golem.isWaiting()) return false;
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        cooldown = 20;
        work = JobWork.of(golem.getJob());
        return work != null && plan();
    }

    private boolean plan() {
        ServerLevel level = level();
        WorkArea area = work.area(golem);
        if (!level.hasChunksAt(area.min(), area.max())) return false;
        skipped.values().removeIf(t -> t <= level.getGameTime());
        ItemStack carried = golem.getCarried();
        boolean full = !carried.isEmpty() && carried.getCount() >= carried.getMaxStackSize();
        ItemEntity loose = full ? null : nearestItem(area, carried);
        if (!full && loose != null) return begin(Task.PICKUP, loose.blockPosition(), loose);
        if (!carried.isEmpty() && work.deliverFirst() && (full || otherItems(area, carried))) {
            BlockPos chest = work.dropOff(golem, level, area, carried, this::skip);
            if (chest != null) return begin(Task.DELIVER, chest, null);
        }
        if (!full) {
            BlockPos target = work.findTarget(golem, level, area, this::skip);
            if (target != null) return begin(Task.WORK, target, null);
        }
        if (!carried.isEmpty()) {
            BlockPos chest = work.dropOff(golem, level, area, carried, this::skip);
            if (chest != null) return begin(Task.DELIVER, chest, null);
        }
        return false;
    }

    private @Nullable ItemEntity nearestItem(WorkArea area, ItemStack carried) {
        List<ItemEntity> items = golem.level().getEntitiesOfClass(ItemEntity.class, area.box(),
                e -> e.isAlive() && !e.hasPickUpDelay() && work.wants(e) && Storage.fits(carried, e.getItem()) && !skip(e.blockPosition()));
        return items.stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
    }

    private boolean otherItems(WorkArea area, ItemStack carried) {
        return !golem.level().getEntitiesOfClass(ItemEntity.class, area.box(),
                e -> e.isAlive() && work.wants(e) && !Storage.fits(carried, e.getItem())).isEmpty();
    }

    private boolean begin(Task t, BlockPos p, @Nullable ItemEntity e) {
        task = t;
        pos = p;
        item = e;
        ticks = 0;
        workTicks = 0;
        failed = false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (task == null || failed || golem.isWaiting() || ticks > GIVE_UP) return false;
        return switch (task) {
            case PICKUP -> item != null && item.isAlive() && Storage.fits(golem.getCarried(), item.getItem());
            case DELIVER -> Storage.containerAt(level(), pos) != null && !golem.getCarried().isEmpty();
            case WORK -> work.isValid(level(), pos);
        };
    }

    @Override
    public void start() {
        moveTo();
    }

    private void moveTo() {
        Path path = task == Task.PICKUP ? golem.getNavigation().createPath(item, 0) : golem.getNavigation().createPath(pos, 1);
        if (path == null) {
            if (golem.onGround() && !golem.isInWater()) failed = true;
            return;
        }
        golem.getNavigation().moveTo(path, 1.0);
    }

    private boolean inReach() {
        if (task == Task.PICKUP) return golem.distanceToSqr(item) < 2.0;
        Vec3 c = Vec3.atCenterOf(pos);
        double dx = golem.getX() - c.x, dz = golem.getZ() - c.z, dy = c.y - golem.getY();
        return dx * dx + dz * dz < 6.25 && dy > -2.0 && dy < 3.0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (task == null || failed) return;
        if (!golem.usePower(1)) {
            task = null;
            return;
        }
        ticks++;
        if (task == Task.PICKUP && item != null) pos = item.blockPosition();
        golem.getLookControl().setLookAt(Vec3.atCenterOf(pos));
        if (!inReach()) {
            if (golem.getNavigation().isDone() || ticks % 40 == 0) moveTo();
            return;
        }
        golem.getNavigation().stop();
        switch (task) {
            case PICKUP -> {
                Storage.pickUp(golem, item);
                for (ItemEntity near : golem.level().getEntitiesOfClass(ItemEntity.class, golem.getBoundingBox().inflate(2.5),
                        e -> e.isAlive() && !e.hasPickUpDelay() && work.wants(e))) {
                    Storage.pickUp(golem, near);
                }
                task = null;
            }
            case DELIVER -> {
                Container c = Storage.containerAt(level(), pos);
                if (c != null) {
                    golem.setCarried(Storage.insert(c, golem.getCarried()));
                    golem.swing(InteractionHand.OFF_HAND);
                    golem.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.6F, 1.3F);
                }
                if (!golem.getCarried().isEmpty()) skipped.put(pos, level().getGameTime() + 100);
                task = null;
            }
            case WORK -> {
                int need = work.workTicks(golem, level(), pos);
                if (workTicks % 10 == 0) golem.swing(InteractionHand.MAIN_HAND);
                workTicks++;
                if (need > 20) level().destroyBlockProgress(golem.getId(), pos, workTicks * 10 / need);
                if (workTicks >= need) {
                    level().destroyBlockProgress(golem.getId(), pos, -1);
                    work.finish(golem, level(), pos);
                    task = null;
                }
            }
        }
    }

    @Override
    public void stop() {
        if (pos != null) {
            if (task == Task.WORK) level().destroyBlockProgress(golem.getId(), pos, -1);
            if (task != null && (failed || ticks > GIVE_UP)) skipped.put(pos, level().getGameTime() + SKIP_FOR);
        }
        task = null;
        item = null;
        golem.getNavigation().stop();
        cooldown = 0;
    }
}
