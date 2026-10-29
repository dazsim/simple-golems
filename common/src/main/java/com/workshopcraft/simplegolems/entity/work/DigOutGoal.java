package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

// stuck somewhere with no way back: dig a staircase towards home or the work area until it can walk again
public class DigOutGoal extends Goal {
    private static final int CHECK = 40;
    private static final int TRIES = 3;
    private static final int RETRY = 200;
    private final GolemEntity golem;
    private int failures;
    private long nextCheck;
    private long restUntil;
    private @Nullable BlockPos next;
    private @Nullable BlockPos working;
    private int workTicks;
    private int moving;
    private boolean done;
    private long nextFree;
    private int turn;

    public DigOutGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    private ServerLevel level() {
        return (ServerLevel) golem.level();
    }

    private WorkArea area() {
        JobWork work = JobWork.of(golem.getJob());
        return work == null ? golem.getArea() : work.area(golem);
    }

    // a miner down its own pit
    private boolean inPit() {
        if (golem.getJob() != Job.MINER || golem.getCustomArea() == null) return false;
        BlockPos p = golem.blockPosition(), min = area().min(), max = area().max();
        return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getZ() >= min.getZ() && p.getZ() <= max.getZ()
                && p.getY() >= min.getY() && p.getY() <= max.getY() + 1;
    }

    // home when it has no marked area, the nearest step of its staircase down a pit, otherwise just above the nearest column of the area
    private BlockPos anchor() {
        if (golem.getCustomArea() == null) return golem.getHome();
        if (inPit()) return MinerWork.INSTANCE.stairsNear(golem, level());
        WorkArea a = area();
        BlockPos min = a.min(), max = a.max();
        return new BlockPos(Mth.clamp(golem.getBlockX(), min.getX(), max.getX()), max.getY() + 1, Mth.clamp(golem.getBlockZ(), min.getZ(), max.getZ()));
    }

    private boolean free() {
        BlockPos a = anchor();
        if (inPit()) {
            // just under the rim it can dig the top layers and climb from where it stands
            if (golem.getBlockY() >= area().max().getY() - 1 || golem.blockPosition().distManhattan(a) <= 1) return true;
            Path p = golem.getNavigation().createPath(a, 0);
            return p != null && p.canReach();
        }
        if (golem.blockPosition().distManhattan(a) <= 2) return true;
        if (golem.getCustomArea() != null && area().contains(golem.blockPosition())) return true;
        Path p = golem.getNavigation().createPath(a, 1);
        return p != null && p.canReach();
    }

    @Override
    public boolean canUse() {
        long now = level().getGameTime();
        if (now < nextCheck || now < restUntil) return false;
        nextCheck = now + CHECK;
        if (!Config.digOut || golem.getJob() == Job.BLANK || golem.getJob() == Job.GUARD || golem.isWaiting() || !golem.onGround()
                || !inPit() && area().near(golem.blockPosition(), 1) || free()) {
            failures = 0;
            return false;
        }
        return ++failures >= TRIES;
    }

    @Override
    public boolean canContinueToUse() {
        return !done;
    }

    @Override
    public void start() {
        golem.getNavigation().stop();
        done = false;
        next = null;
        working = null;
        moving = 0;
        nextFree = 0;
    }

    @Override
    public void stop() {
        failures = 0;
        if (working != null) level().destroyBlockProgress(golem.getId(), working, -1);
        working = null;
        next = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ServerLevel level = level();
        if (working != null) {
            golem.getLookControl().setLookAt(Vec3.atCenterOf(working));
            int need = Config.scaled(digTicks(working));
            if (workTicks % 10 == 0) golem.swing(InteractionHand.MAIN_HAND);
            if (++workTicks < need) {
                if (need > 20) level.destroyBlockProgress(golem.getId(), working, workTicks * 10 / need);
                return;
            }
            level.destroyBlockProgress(golem.getId(), working, -1);
            dig(working);
            working = null;
            return;
        }
        if (next != null) {
            boolean there = golem.blockPosition().equals(next) && golem.onGround();
            if (there || ++moving > 60) {
                if (there) turn++;
                next = null;
                moving = 0;
                return;
            }
            golem.getMoveControl().setWantedPosition(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 1.0);
            if (next.getY() > golem.getBlockY() && golem.onGround()) golem.getJumpControl().jump();
            return;
        }
        // on its own timer: a step cycle in step with the game clock must not skip every check
        if (level.getGameTime() >= nextFree) {
            nextFree = level.getGameTime() + 20;
            if (free()) {
                golem.markFreed();
                done = true;
                return;
            }
        }
        BlockPos here = golem.blockPosition();
        for (BlockPos step : candidates(here)) {
            if (!passable(here, step)) continue;
            if (prepare(here, step)) next = step;
            moving = 0;
            return;
        }
        // walled in by things it must not break: try again later
        restUntil = level.getGameTime() + RETRY;
        done = true;
    }

    // towards the anchor, up or down one block with each step; straight up or down it winds round a 3 by 3 square
    private List<BlockPos> candidates(BlockPos here) {
        BlockPos a = anchor();
        int dx = a.getX() - here.getX(), dz = a.getZ() - here.getZ(), dy = Integer.signum(a.getY() - here.getY());
        Direction first, second;
        if (dy != 0 && Math.abs(dx) + Math.abs(dz) <= 2) {
            first = Direction.from2DDataValue(turn / 2 % 4);
            second = first.getClockWise();
        } else {
            first = Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST) : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
            second = Math.abs(dx) >= Math.abs(dz) ? (dz >= 0 ? Direction.SOUTH : Direction.NORTH) : (dx >= 0 ? Direction.EAST : Direction.WEST);
        }
        List<BlockPos> out = new ArrayList<>();
        for (Direction d : new Direction[]{first, second, second.getOpposite(), first.getOpposite()}) out.add(here.relative(d).above(dy));
        if (dy != 0) for (Direction d : new Direction[]{first, second, second.getOpposite()}) out.add(here.relative(d));
        return out;
    }

    // the two blocks of the step, plus room over its head when climbing
    private static List<BlockPos> room(BlockPos here, BlockPos step) {
        List<BlockPos> r = new ArrayList<>(List.of(step, step.above()));
        if (step.getY() > here.getY()) r.add(here.above());
        return r;
    }

    private boolean passable(BlockPos here, BlockPos step) {
        ServerLevel level = level();
        boolean blocks = golem.carries(MinerWork::isBuildingBlock);
        for (BlockPos p : room(here, step)) {
            if (!open(p) && !breakable(p)) return false;
            if (!blocks && hot(p)) return false;
            for (Direction d : Direction.values()) {
                if (!blocks && hot(p.relative(d))) return false;
            }
        }
        // something to stand on, or a block to build it with
        BlockPos floor = step.below();
        BlockState f = level.getBlockState(floor);
        if (f.isFaceSturdy(level, floor, Direction.UP) && !hot(floor)) return true;
        if (inPit() && MinerWork.isHeadroom(area(), floor)) return false;
        return blocks && (f.canBeReplaced() || breakable(floor));
    }

    // one thing at a time: clear the step, seal any water or lava round it, build the floor, then walk on to it
    private boolean prepare(BlockPos here, BlockPos step) {
        ServerLevel level = level();
        List<BlockPos> room = room(here, step);
        for (BlockPos p : room) {
            if (!level.getFluidState(p).isEmpty() && level.getBlockState(p).canBeReplaced()) {
                if (place(p)) return false;
                continue;
            }
            if (!open(p)) {
                working = p.immutable();
                workTicks = 0;
                return false;
            }
        }
        for (BlockPos p : room) {
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (room.contains(n) || golem.getBoundingBox().intersects(new AABB(n))) continue;
                if (!level.getFluidState(n).isEmpty() && level.getBlockState(n).canBeReplaced() && place(n)) return false;
            }
        }
        BlockPos floor = step.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP) && level.getBlockState(floor).canBeReplaced()) {
            if (place(floor)) return false;
        }
        return true;
    }

    private boolean place(BlockPos p) {
        // never into the way up its own staircase, unless it is sealing water or lava there
        if (inPit() && MinerWork.isHeadroom(area(), p) && level().getFluidState(p).isEmpty()) return false;
        int slot = MinerWork.INSTANCE.keep(golem);
        if (slot < 0) return false;
        ItemStack carried = golem.getCarried().get(slot);
        BlockState state = ((BlockItem) carried.getItem()).getBlock().defaultBlockState();
        level().setBlockAndUpdate(p, state);
        level().playSound(null, p, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
        carried.shrink(1);
        golem.carriedChanged();
        golem.swing(InteractionHand.OFF_HAND);
        return true;
    }

    private boolean open(BlockPos p) {
        return BuildUp.open(level(), p);
    }

    private boolean hot(BlockPos p) {
        return BuildUp.hot(level(), p);
    }

    // never bedrock or anything unbreakable, never a chest or other block entity, never inside another golem's area.
    // Down its own pit a miner only digs the pit, never its staircase
    private boolean breakable(BlockPos p) {
        ServerLevel level = level();
        BlockState s = level.getBlockState(p);
        if (s.isAir() || s.hasBlockEntity() || s.getDestroySpeed(level, p) < 0 || !s.getFluidState().isEmpty()) return false;
        if (inPit() && (!area().contains(p) || MinerWork.isStair(area(), p) || s.is(ModTags.MINER_NEVER_DIG))) return false;
        return !BuildUp.inOtherArea(golem, p);
    }

    // a miner digs with its pickaxe, anyone else by hand
    private int digTicks(BlockPos p) {
        BlockState s = level().getBlockState(p);
        if (golem.getJob() == Job.MINER && !golem.getTool().isEmpty()) return golem.digTicks(s, p);
        float hardness = s.getDestroySpeed(level(), p);
        return hardness <= 0 ? 2 : Mth.clamp(Mth.ceil(hardness * 30.0F), 2, 200) + 2;
    }

    private void dig(BlockPos p) {
        ServerLevel level = level();
        BlockState s = level.getBlockState(p);
        if (open(p) || !breakable(p)) return;
        boolean pick = golem.getJob() == Job.MINER && !golem.getTool().isEmpty();
        ItemStack tool = pick ? golem.getTool() : ItemStack.EMPTY;
        List<ItemStack> drops = (pick ? golem.canHarvest(s) : !s.requiresCorrectToolForDrops()) ? Block.getDrops(s, level, p, null, golem, tool) : List.of();
        LumberWork.breakQuietly(level, p, s, true);
        if (pick) tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
        Vec3 at = Vec3.atCenterOf(p);
        for (ItemStack d : LumberWork.merge(drops)) Storage.collectOrDrop(golem, level, at, d);
    }
}
