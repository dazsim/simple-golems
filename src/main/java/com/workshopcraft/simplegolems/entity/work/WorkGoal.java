package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.Config;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Node;
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
    private enum Task { PICKUP, DELIVER, WORK, FETCH, TIDY }

    private static final int GIVE_UP = 400;
    private static final int SKIP_FOR = 1200;
    private static final int RETRY_ROUTE = 200;
    private static final int STUCK = 80;
    private static final int MAX_TRIP = 9000;

    private final GolemEntity golem;
    private final Map<BlockPos, Long> skipped = new HashMap<>();
    // storage it could not get to: tried again soon, and not a reason for the chest bubble
    private final Map<BlockPos, Long> unreachable = new HashMap<>();
    // how often a block has been out of reach: it waits longer each time so the rest of the work goes on
    private final Map<BlockPos, Integer> misses = new HashMap<>();
    private long retryMissed;
    private int freed;
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
    // only out of reach from where it got to, so worth another go soon
    private boolean outOfReach;
    // where it last moved to, and how long it has stood there
    private @Nullable Vec3 lastSpot;
    private int still;
    // building steps up to a target it cannot walk to, and what it is fetching from storage for them
    private @Nullable BuildUp climb;
    private @Nullable BlockPos climbFor;
    private @Nullable BlockPos needFor;
    private @Nullable Predicate<ItemStack> fetching;

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
    private @Nullable BlockPos storageFor(ServerLevel level, WorkArea area, List<ItemStack> load) {
        BlockPos chest = dropOff(level, area, load, this::skipOrAway);
        golem.setStorageTrouble(chest == null && dropOff(level, area, load, this::skip) == null);
        return chest;
    }

    private @Nullable BlockPos dropOff(ServerLevel level, WorkArea area, List<ItemStack> load, Predicate<BlockPos> skip) {
        for (ItemStack c : load) {
            BlockPos p = work.dropOff(golem, level, area, c, skip);
            if (p != null) return p;
        }
        return null;
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
        if (freed != golem.freed()) {
            freed = golem.freed();
            skipped.clear();
            unreachable.clear();
            misses.clear();
        }
        skipped.values().removeIf(t -> t <= level.getGameTime());
        unreachable.values().removeIf(t -> t <= level.getGameTime());
        if (climbFor != null && !work.isValid(golem, level, climbFor)) climbFor = null;
        List<BlockPos> steps = golem.climbSteps();
        steps.removeIf(p -> !isStep(level, p));
        if (work.climbSupplies().equals(golem.getSupplies())
                && (needFor == null || !work.isValid(golem, level, needFor) || climbBlocksNear(level, area))) {
            golem.setSupplies(null);
            needFor = null;
        }
        // the steps come down once the work up there is done, top first so it walks down them
        if (climbFor == null && !steps.isEmpty()) {
            BlockPos step = steps.stream().filter(p -> !skip(p))
                    .min(Comparator.comparingInt((BlockPos p) -> -p.getY()).thenComparingDouble(p -> golem.distanceToSqr(Vec3.atCenterOf(p)))).orElse(null);
            if (step != null) return begin(Task.TIDY, step, null);
        }
        List<ItemEntity> loose = level.getEntitiesOfClass(ItemEntity.class, itemBox(area),
                e -> e.isAlive() && work.wants(e) && !skip(e.blockPosition()));
        for (ItemEntity e : loose) {
            if (e.getAge() > 3000) e.setExtendedLifetime();
        }
        boolean full = golem.carryFull();
        ItemEntity next = loose.stream()
                .filter(e -> !e.hasPickUpDelay() && golem.canCarry(e.getItem()))
                .min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        if (next != null) return begin(Task.PICKUP, next.blockPosition(), next);
        // off to storage once the slots are full or nothing is left to do; every drop is cleared before the next tree, crop or block
        List<ItemStack> load = work.load(golem);
        if (!load.isEmpty()) {
            // nowhere to put it: stop and wait rather than spill more drops
            BlockPos chest = storageFor(level, area, load);
            if (chest == null) return false;
            boolean stuck = !loose.isEmpty() && loose.stream().noneMatch(e -> golem.canCarry(e.getItem()));
            if (full || stuck) return begin(Task.DELIVER, chest, null);
        } else {
            golem.setStorageTrouble(false);
        }
        if (!loose.isEmpty()) return false;
        if (!full) {
            BlockPos target = work.findTarget(golem, level, area, this::skipOrStep);
            // nothing else left: blocks that were out of reach get another go, the way may be open now
            if (target == null && level.getGameTime() >= retryMissed && skipped.keySet().removeIf(misses::containsKey)) {
                retryMissed = level.getGameTime() + RETRY_ROUTE;
                target = work.findTarget(golem, level, area, this::skipOrStep);
            }
            if (target != null) return begin(Task.WORK, target, null);
        }
        if (!load.isEmpty()) {
            BlockPos chest = storageFor(level, area, load);
            if (chest != null) return begin(Task.DELIVER, chest, null);
        } else if (work.restock(golem) != null && !full) {
            Predicate<ItemStack> want = work.restock(golem);
            BlockPos chest = Storage.nearest(level, area, golem.position(), c -> c.hasAnyMatching(want), this::skipOrAway);
            if (work.supplies() == null) golem.setBlocksTrouble(chest == null);
            else golem.setSupplies(chest == null ? work.supplies() : null);
            if (chest != null) return begin(Task.FETCH, chest, null);
        }
        return false;
    }

    private boolean skipOrStep(BlockPos p) {
        return skip(p) || golem.climbSteps().contains(p);
    }

    // a step it built that is still there
    private boolean isStep(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.isAir() || s.hasBlockEntity()) return false;
        Predicate<ItemStack> with = work.climbWith(golem);
        return with == null || with.test(new ItemStack(s.getBlock().asItem()));
    }

    private boolean climbBlocksNear(ServerLevel level, WorkArea area) {
        Predicate<ItemStack> with = work.climbWith(golem);
        return with != null && (golem.carries(with) || Storage.nearest(level, area, golem.position(), c -> c.hasAnyMatching(with), p -> false) != null);
    }

    // a target above its reach that it could build steps up to
    private boolean canClimb() {
        return task == Task.WORK && work.climbWith(golem) != null && level().getGameRules().get(GameRules.MOB_GRIEFING)
                && Vec3.atCenterOf(pos).y - golem.getY() >= 4.0;
    }

    private void startClimb() {
        golem.getNavigation().stop();
        climb = new BuildUp(golem, work.climbWith(golem), p -> work.climbClears(level(), p));
        climbFor = pos;
    }

    private void tickClimb() {
        switch (climb.tick(pos)) {
            case CLIMBING -> ticks = 0;
            case NO_BLOCKS -> {
                Predicate<ItemStack> with = work.climbWith(golem);
                BlockPos chest = Storage.nearest(level(), work.area(golem), golem.position(), c -> c.hasAnyMatching(with), this::skipOrAway);
                if (chest != null) {
                    begin(Task.FETCH, chest, null);
                    fetching = with;
                    moveTo();
                } else {
                    golem.setSupplies(work.climbSupplies());
                    needFor = pos;
                    failed = true;
                    outOfReach = true;
                }
            }
            case BLOCKED -> {
                failed = true;
                outOfReach = true;
            }
        }
    }

    private void breakStep(BlockPos p) {
        ServerLevel level = level();
        BlockState s = level.getBlockState(p);
        golem.climbSteps().remove(p);
        if (!isStep(level, p)) return;
        List<ItemStack> drops = Block.getDrops(s, level, p, null, golem, golem.getTool());
        LumberWork.breakQuietly(level, p, s, true);
        for (ItemStack d : LumberWork.merge(drops)) Storage.collectOrDrop(golem, level, Vec3.atCenterOf(p), d);
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
        outOfReach = false;
        lastSpot = null;
        still = 0;
        climb = null;
        fetching = null;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (task == null || failed || golem.isWaiting() || ticks > GIVE_UP || trip > MAX_TRIP) return false;
        return switch (task) {
            case PICKUP -> item != null && item.isAlive() && golem.canCarry(item.getItem());
            case DELIVER -> Storage.containerAt(level(), pos) != null && !work.load(golem).isEmpty();
            case FETCH -> Storage.containerAt(level(), pos) != null && !golem.carryFull();
            case WORK -> work.isValid(golem, level(), pos);
            case TIDY -> golem.climbSteps().contains(pos) && isStep(level(), pos);
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
        if (path == null || !path.canReach() && !endsInReach(path)) {
            BlockPos hop = work.waypoint(golem, level(), task == Task.PICKUP ? item.blockPosition() : pos);
            Path p = hop == null || golem.blockPosition().closerThan(hop, 1.5) ? null : golem.getNavigation().createPath(hop, 0);
            if (p != null && p.canReach()) path = p;
            else if (task == Task.WORK && golem.onGround()) {
                // too high to walk up to: go as near as it can, then build steps
                if (canClimb()) {
                    if (path != null) golem.getNavigation().moveTo(path, 1.0);
                    else startClimb();
                    return;
                }
                // no way to get near this block from here: on to the next one
                failed = true;
                outOfReach = true;
                return;
            } else if (p != null) path = p;
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
        if (task == Task.TIDY) return golem.getNavigation().createPath(pos.above(), 0);
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

    private boolean endsInReach(Path path) {
        Node end = path.getEndNode();
        return end != null && inReach(end.x + 0.5, end.y, end.z + 0.5);
    }

    private boolean inReach() {
        if (task == Task.PICKUP) return golem.distanceToSqr(item) < 2.0;
        return inReach(golem.getX(), golem.getY(), golem.getZ());
    }

    private boolean inReach(double x, double y, double z) {
        Vec3 c = Vec3.atCenterOf(pos);
        if (task == Task.TIDY) return c.distanceToSqr(x, y + 0.45, z) < 30.0 && y < pos.getY() + 1.5;
        double dx = x - c.x, dz = z - c.z, dy = c.y - y;
        return dx * dx + dz * dz < 6.25 && dy > -2.0 && dy < (task == Task.WORK ? 4.0 : 3.0);
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
            if (climb != null) {
                tickClimb();
                return;
            }
            // walked as far as it can and got no closer: out of reach for now
            if (lastSpot == null || golem.position().distanceToSqr(lastSpot) > 0.25) {
                lastSpot = golem.position();
                still = 0;
            }
            ++still;
            if (canClimb() && (golem.getNavigation().isDone() && still > 20 || still > STUCK)) {
                startClimb();
                return;
            }
            if (ticks > STUCK && golem.getNavigation().isDone() || still > STUCK) {
                failed = true;
                outOfReach = true;
                return;
            }
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
                    int keep = work.keep(golem);
                    for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) {
                        ItemStack s = golem.getCarried().get(i);
                        if (work.keeps(golem, i)) continue;
                        if (i != keep) golem.getCarried().set(i, Storage.insert(c, s));
                        else if (s.getCount() > work.reserve(golem)) s.grow(Storage.insert(c, s.split(s.getCount() - work.reserve(golem))).getCount());
                    }
                    golem.carriedChanged();
                    golem.swing(InteractionHand.OFF_HAND);
                    golem.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.6F, 1.3F);
                }
                if (!work.load(golem).isEmpty()) skipped.put(pos, level().getGameTime() + 100);
                task = null;
            }
            case FETCH -> {
                Container c = Storage.containerAt(level(), pos);
                Predicate<ItemStack> want = fetching != null ? fetching : work.restock(golem);
                if (c != null && want != null) {
                    // up to one full stack of the first kind it finds
                    ItemStack got = ItemStack.EMPTY;
                    int limit = Math.max(16, Config.blockReserve);
                    for (int i = 0; i < c.getContainerSize() && got.getCount() < limit; i++) {
                        ItemStack s = c.getItem(i);
                        if (s.isEmpty() || !want.test(s) || !got.isEmpty() && !ItemStack.isSameItemSameComponents(got, s)) continue;
                        ItemStack taken = c.removeItem(i, Math.min(s.getCount(), Math.min(limit, s.getMaxStackSize()) - got.getCount()));
                        if (got.isEmpty()) got = taken;
                        else got.grow(taken.getCount());
                    }
                    if (!got.isEmpty()) {
                        golem.addCarried(got);
                        if (!got.isEmpty()) Storage.insert(c, got);
                        c.setChanged();
                        golem.swing(InteractionHand.OFF_HAND);
                        golem.playSound(SoundEvents.BUNDLE_INSERT, 0.6F, 1.2F);
                    }
                }
                task = null;
            }
            case WORK -> {
                int need = Config.scaled(work.workTicks(golem, level(), pos));
                if (workTicks % 10 == 0) golem.swing(InteractionHand.MAIN_HAND);
                workTicks++;
                if (need > 20) level().destroyBlockProgress(golem.getId(), pos, workTicks * 10 / need);
                if (workTicks >= need) {
                    level().destroyBlockProgress(golem.getId(), pos, -1);
                    work.finish(golem, level(), pos);
                    task = null;
                }
            }
            case TIDY -> {
                golem.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                int need = Config.scaled(golem.digTicks(level().getBlockState(pos), pos));
                if (workTicks % 10 == 0) golem.swing(InteractionHand.MAIN_HAND);
                workTicks++;
                if (need > 20) level().destroyBlockProgress(golem.getId(), pos, workTicks * 10 / need);
                if (workTicks >= need) {
                    level().destroyBlockProgress(golem.getId(), pos, -1);
                    breakStep(pos);
                    task = null;
                }
            }
        }
    }

    @Override
    public void stop() {
        if (pos != null) {
            if (task == Task.WORK || task == Task.TIDY) level().destroyBlockProgress(golem.getId(), pos, -1);
            if (task != null && (failed || ticks > GIVE_UP || trip > MAX_TRIP)) {
                if (task == Task.DELIVER || task == Task.FETCH) unreachable.put(pos, level().getGameTime() + RETRY_ROUTE);
                else if (outOfReach) {
                    if (misses.size() > 1024) misses.clear();
                    int n = misses.merge(pos, 1, Integer::sum);
                    skipped.put(pos, level().getGameTime() + Math.min(RETRY_ROUTE << Math.min(n - 1, 5), SKIP_FOR * 5));
                } else skipped.put(pos, level().getGameTime() + SKIP_FOR);
            }
        }
        if (task != Task.FETCH || fetching == null) climbFor = null;
        task = null;
        item = null;
        climb = null;
        fetching = null;
        golem.getNavigation().stop();
        cooldown = 0;
    }
}
