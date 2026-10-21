package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class WorkGoal extends Goal {
    private enum Task { PICKUP, DELIVER, WORK, FETCH }

    private static final int GIVE_UP = 400;
    private static final int SKIP_FOR = 1200;
    private static final int RETRY_ROUTE = 200;
    private static final int MAX_TRIP = 9000;

    private final GolemEntity golem;
    private final Map<BlockPos, Long> skipped = new HashMap<>();
    // storage it could not get to: tried again soon, and not a reason for the chest bubble
    private final Map<BlockPos, Long> unreachable = new HashMap<>();
    private @Nullable Task task;
    private @Nullable JobWork work;
    private @Nullable BlockPos pos;
    private @Nullable ItemEntity item;
    private int ticks;
    private int trip;
    private double best;
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

    private boolean skipOrAway(BlockPos p) {
        Long until = unreachable.get(p);
        return skip(p) || until != null && until > golem.level().getGameTime();
    }

    // a container it can use, or null; the chest bubble only shows when there is none at all, not when it is out of reach
    private @Nullable BlockPos storageFor(ServerLevel level, WorkArea area, ItemStack carried) {
        BlockPos chest = work.dropOff(golem, level, area, carried, this::skipOrAway);
        golem.setStorageTrouble(chest == null && work.dropOff(golem, level, area, carried, this::skip) == null);
        return chest;
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
        unreachable.values().removeIf(t -> t <= level.getGameTime());
        List<ItemEntity> loose = level.getEntitiesOfClass(ItemEntity.class, itemBox(area),
                e -> e.isAlive() && work.wants(e) && !skip(e.blockPosition()));
        for (ItemEntity e : loose) {
            if (e.getAge() > 3000) e.setExtendedLifetime();
        }
        ItemStack carried = golem.getCarried();
        boolean full = !carried.isEmpty() && carried.getCount() >= carried.getMaxStackSize();
        ItemEntity next = full ? null : loose.stream()
                .filter(e -> !e.hasPickUpDelay() && Storage.fits(carried, e.getItem()))
                .min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        if (next != null) return begin(Task.PICKUP, next.blockPosition(), next);
        // one item type per trip, and every drop is cleared before the next tree, crop or block
        if (!carried.isEmpty()) {
            // nowhere to put it: stop and wait rather than spill more drops
            BlockPos chest = storageFor(level, area, carried);
            if (chest == null) return false;
            if (full || !loose.isEmpty()) return begin(Task.DELIVER, chest, null);
        } else {
            golem.setStorageTrouble(false);
        }
        if (!loose.isEmpty()) return false;
        if (!full) {
            BlockPos target = work.findTarget(golem, level, area, this::skip);
            if (target != null) return begin(Task.WORK, target, null);
        }
        if (!carried.isEmpty()) {
            BlockPos chest = storageFor(level, area, carried);
            if (chest != null) return begin(Task.DELIVER, chest, null);
        } else if (work.restock(golem) != null) {
            Predicate<ItemStack> want = work.restock(golem);
            BlockPos chest = Storage.nearest(level, area, golem.position(), c -> c.hasAnyMatching(want), this::skipOrAway);
            golem.setBlocksTrouble(chest == null);
            if (chest != null) return begin(Task.FETCH, chest, null);
        }
        return false;
    }

    private static AABB itemBox(WorkArea area) {
        return area.box().inflate(2, 1, 2);
    }

    private boolean begin(Task t, BlockPos p, @Nullable ItemEntity e) {
        task = t;
        pos = p;
        item = e;
        ticks = 0;
        trip = 0;
        best = Double.MAX_VALUE;
        workTicks = 0;
        failed = false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (task == null || failed || golem.isWaiting() || ticks > GIVE_UP || trip > MAX_TRIP) return false;
        return switch (task) {
            case PICKUP -> item != null && item.isAlive() && Storage.fits(golem.getCarried(), item.getItem());
            case DELIVER -> Storage.containerAt(level(), pos) != null && !golem.getCarried().isEmpty();
            case FETCH -> Storage.containerAt(level(), pos) != null && golem.getCarried().isEmpty();
            case WORK -> work.isValid(golem, level(), pos);
        };
    }

    @Override
    public void start() {
        moveTo();
    }

    private void moveTo() {
        Path path = task == Task.PICKUP ? golem.getNavigation().createPath(item, 0)
                : task == Task.WORK && work.fromGround() ? groundPath(pos) : workPath();
        // too far or too deep for one path: hop along the way (the miner's own staircase)
        if (path == null || !path.canReach()) {
            BlockPos hop = work.waypoint(golem, level(), task == Task.PICKUP ? item.blockPosition() : pos);
            Path p = hop == null ? null : golem.getNavigation().createPath(hop, 0);
            if (p != null) path = p;
        }
        if (path == null) {
            if (golem.onGround() && !golem.isInWater()) failed = true;
            return;
        }
        golem.getNavigation().moveTo(path, 1.0);
    }

    // ground navigation aims at the first open block above a solid target, which for a trunk is the treetop,
    // so walk to an open spot beside it instead
    private @Nullable Path workPath() {
        BlockPos at = task == Task.WORK ? work.approach(golem, level(), pos) : null;
        return at != null ? golem.getNavigation().createPath(at, 0) : golem.getNavigation().createPath(pos, 1);
    }

    private @Nullable Path groundPath(BlockPos target) {
        ServerLevel level = level();
        Path fallback = null;
        List<BlockPos> spots = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(target.offset(-1, 0, -1), target.offset(1, 0, 1))) {
            if (p.equals(target)) continue;
            BlockPos below = p.below();
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                spots.add(p.immutable());
            }
        }
        spots.sort(Comparator.comparingDouble(p -> golem.distanceToSqr(Vec3.atBottomCenterOf(p))));
        for (BlockPos p : spots) {
            Path path = golem.getNavigation().createPath(p, 0);
            if (path != null && path.canReach()) return path;
            if (fallback == null) fallback = path;
        }
        return fallback;
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
        trip++;
        if (task == Task.PICKUP && item != null) pos = item.blockPosition();
        // still getting closer, so a long walk is not a stuck one
        double d = golem.distanceToSqr(Vec3.atCenterOf(pos));
        if (d < best - 1.0) {
            best = d;
            ticks = 0;
        }
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
            case FETCH -> {
                Container c = Storage.containerAt(level(), pos);
                Predicate<ItemStack> want = work.restock(golem);
                if (c != null && want != null) {
                    for (int i = 0; i < c.getContainerSize(); i++) {
                        ItemStack s = c.getItem(i);
                        if (!s.isEmpty() && want.test(s)) {
                            golem.setCarried(c.removeItem(i, Math.min(s.getCount(), 32)));
                            c.setChanged();
                            golem.swing(InteractionHand.OFF_HAND);
                            golem.playSound(SoundEvents.BUNDLE_INSERT, 0.6F, 1.2F);
                            break;
                        }
                    }
                }
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
            if (task != null && (failed || ticks > GIVE_UP || trip > MAX_TRIP)) {
                if (task == Task.DELIVER || task == Task.FETCH) unreachable.put(pos, level().getGameTime() + RETRY_ROUTE);
                else skipped.put(pos, level().getGameTime() + SKIP_FOR);
            }
        }
        task = null;
        item = null;
        golem.getNavigation().stop();
        cooldown = 0;
    }
}
