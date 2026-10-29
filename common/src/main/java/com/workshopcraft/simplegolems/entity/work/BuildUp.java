package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// builds steps out of carried blocks up to a target it cannot walk to; the steps are kept on the golem so they can be taken down after
public class BuildUp {
    public enum Result { CLIMBING, NO_BLOCKS, BLOCKED }

    private static final int MAX_STEPS = 24;
    private static final int MAX_TICKS = 1200;
    private final GolemEntity golem;
    private final Predicate<ItemStack> blocks;
    private final Predicate<BlockPos> clears;
    private @Nullable BlockPos next;
    private int moving;
    private int turn;
    private int ticks;
    private int built;

    public BuildUp(GolemEntity golem, Predicate<ItemStack> blocks, Predicate<BlockPos> clears) {
        this.golem = golem;
        this.blocks = blocks;
        this.clears = clears;
    }

    private ServerLevel level() {
        return (ServerLevel) golem.level();
    }

    public Result tick(BlockPos target) {
        if (++ticks > MAX_TICKS || built > MAX_STEPS) return Result.BLOCKED;
        if (next != null) {
            boolean there = golem.blockPosition().equals(next) && golem.onGround();
            if (there || ++moving > 60) {
                if (there) turn++;
                next = null;
                moving = 0;
                return Result.CLIMBING;
            }
            golem.getMoveControl().setWantedPosition(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 1.0);
            if (next.getY() > golem.getBlockY() && golem.onGround()) golem.getJumpControl().jump();
            return Result.CLIMBING;
        }
        if (!golem.onGround()) return Result.CLIMBING;
        BlockPos here = golem.blockPosition();
        boolean wantsBlocks = false;
        for (BlockPos step : candidates(here, target)) {
            int ok = passable(here, step);
            if (ok == 0) continue;
            if (ok < 0) {
                wantsBlocks = true;
                continue;
            }
            if (prepare(step)) {
                next = step;
                moving = 0;
            }
            return Result.CLIMBING;
        }
        return wantsBlocks ? Result.NO_BLOCKS : Result.BLOCKED;
    }

    // level steps towards the target once it is high enough, otherwise one block up, winding round when right under it
    private List<BlockPos> candidates(BlockPos here, BlockPos target) {
        int dx = target.getX() - here.getX(), dz = target.getZ() - here.getZ();
        double rise = Vec3.atCenterOf(target).y - here.getY();
        Direction first, second;
        boolean under = Math.abs(dx) + Math.abs(dz) <= 1;
        if (under) {
            first = Direction.from2DDataValue(turn % 4);
            second = first.getClockWise();
        } else {
            first = Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST) : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
            second = Math.abs(dx) >= Math.abs(dz) ? (dz >= 0 ? Direction.SOUTH : Direction.NORTH) : (dx >= 0 ? Direction.EAST : Direction.WEST);
        }
        List<BlockPos> out = new ArrayList<>();
        if (rise < 3.0 && !under) {
            out.add(here.relative(first));
            if ((first.getAxis() == Direction.Axis.X ? dz : dx) != 0) out.add(here.relative(second));
        }
        if (rise > 0.0) {
            for (Direction d : new Direction[]{first, second, second.getOpposite(), first.getOpposite()}) out.add(here.relative(d).above());
        }
        return out;
    }

    // 1 when it can step there, -1 when it could with a block to build the floor, 0 when not
    private int passable(BlockPos here, BlockPos step) {
        ServerLevel level = level();
        List<BlockPos> room = new ArrayList<>(List.of(step, step.above()));
        if (step.getY() > here.getY()) room.add(here.above());
        for (BlockPos p : room) {
            if (!open(level, p) && !(clears.test(p) && !inOtherArea(golem, p))) return 0;
            if (!level.getFluidState(p).isEmpty()) return 0;
            for (Direction d : Direction.values()) {
                if (hot(level, p.relative(d))) return 0;
            }
        }
        BlockPos floor = step.below();
        BlockState f = level.getBlockState(floor);
        if (hot(level, floor)) return 0;
        if (f.isFaceSturdy(level, floor, Direction.UP)) return 1;
        if (!f.canBeReplaced() || f.hasBlockEntity() || inOtherArea(golem, floor)) return 0;
        if (!level.getEntitiesOfClass(Entity.class, new AABB(floor), e -> !(e instanceof ItemEntity)).isEmpty()) return 0;
        return slot() >= 0 ? 1 : -1;
    }

    // clears the way, then builds the floor; true once it can walk on to the step
    private boolean prepare(BlockPos step) {
        ServerLevel level = level();
        for (BlockPos p : List.of(step, step.above(), golem.blockPosition().above())) {
            if (open(level, p)) continue;
            BlockState s = level.getBlockState(p);
            List<ItemStack> drops = Block.getDrops(s, level, p, null, golem, golem.getTool());
            LumberWork.breakQuietly(level, p, s, true);
            for (ItemStack d : LumberWork.merge(drops)) Storage.collectOrDrop(golem, level, Vec3.atCenterOf(p), d);
            golem.swing(InteractionHand.MAIN_HAND);
            return false;
        }
        BlockPos floor = step.below();
        if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return true;
        if (golem.getBoundingBox().intersects(new AABB(floor))) {
            golem.getMoveControl().setWantedPosition(golem.getBlockX() + 0.5, golem.getY(), golem.getBlockZ() + 0.5, 1.0);
            return false;
        }
        int slot = slot();
        if (slot < 0) return false;
        ItemStack carried = golem.getCarried().get(slot);
        BlockState state = ((BlockItem) carried.getItem()).getBlock().defaultBlockState();
        level.setBlockAndUpdate(floor, state);
        level.playSound(null, floor, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
        carried.shrink(1);
        golem.carriedChanged();
        golem.swing(InteractionHand.OFF_HAND);
        golem.climbSteps().add(floor.immutable());
        built++;
        return true;
    }

    private int slot() {
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack s = golem.getCarried().get(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem && blocks.test(s)) return i;
        }
        return -1;
    }

    public static boolean open(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty() && s.getFluidState().isEmpty() && !s.is(BlockTags.FIRE);
    }

    public static boolean hot(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getFluidState().is(FluidTags.LAVA) || s.is(BlockTags.FIRE);
    }

    public static boolean inOtherArea(GolemEntity golem, BlockPos p) {
        for (GolemEntity other : golem.level().getEntitiesOfClass(GolemEntity.class, golem.getBoundingBox().inflate(64), g -> g != golem && g.getCustomArea() != null)) {
            if (other.getArea().contains(p)) return true;
        }
        return false;
    }
}
