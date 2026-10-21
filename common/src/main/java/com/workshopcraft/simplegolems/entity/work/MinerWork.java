package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// digs the marked box layer by layer from the top. A flat marked area goes all the way down to bedrock.
// It leaves a staircase that winds round the edge of the pit (or runs down the west edge of a narrow one),
// builds missing steps and seals water and lava with plain blocks it has mined.
public class MinerWork implements JobWork {
    public static final MinerWork INSTANCE = new MinerWork();
    public static final TagKey<Item> BUILDING = TagKey.create(Registries.ITEM, SimpleGolems.id("miner_building_blocks"));
    private static final int HOP = 16;

    @Override
    public boolean wants(ItemEntity item) {
        return item.entityTags().contains(Storage.WORK_TAG);
    }

    @Override
    public WorkArea area(GolemEntity golem) {
        WorkArea a = golem.getArea();
        return a.sizeY() == 1 ? a.down(a.min().getY() - golem.level().getMinY() + 1) : a;
    }

    // the staircase: a spiral round the edge when the pit is at least 3 by 3, otherwise straight down the west edge
    record Stairs(int minX, int minZ, int maxX, int maxZ, int top, int bottom) {
        static Stairs of(WorkArea a) {
            return new Stairs(a.min().getX(), a.min().getZ(), a.max().getX(), a.max().getZ(), a.max().getY(), a.min().getY());
        }

        int sx() { return maxX - minX + 1; }
        int sz() { return maxZ - minZ + 1; }
        boolean spiral() { return sx() >= 3 && sz() >= 3; }
        int loop() { return 2 * (sx() + sz()) - 4; }

        int depth(int y) { return top - 1 - y; }

        int[] cell(int i) {
            int sz = sz(), sx = sx();
            if (i < sz) return new int[]{minX, minZ + i};
            i -= sz;
            if (i < sx - 1) return new int[]{minX + 1 + i, maxZ};
            i -= sx - 1;
            if (i < sz - 1) return new int[]{maxX, maxZ - 1 - i};
            i -= sz - 1;
            return new int[]{maxX - 1 - i, minZ};
        }

        int index(int x, int z) {
            if (x == minX) return z - minZ;
            if (z == maxZ) return sz() + (x - minX - 1);
            if (x == maxX) return sz() + sx() - 1 + (maxZ - 1 - z);
            if (z == minZ) return 2 * sz() + sx() - 2 + (maxX - 1 - x);
            return -1;
        }

        // two open blocks above each step so the golem can walk on to the next one
        boolean isStair(BlockPos p) {
            int d = depth(p.getY());
            if (!spiral()) return p.getX() == minX && d >= p.getZ() - minZ;
            int i = index(p.getX(), p.getZ());
            return i >= 0 && d >= i && Math.floorMod(d - i, loop()) < loop() - 2;
        }

        @Nullable BlockPos step(int d) {
            if (d < 0 || top - 1 - d < bottom) return null;
            if (!spiral()) return d < sz() ? new BlockPos(minX, top - 1 - d, minZ + d) : null;
            int[] c = cell(Math.floorMod(d, loop()));
            return new BlockPos(c[0], top - 1 - d, c[1]);
        }
    }

    static boolean isStair(WorkArea area, BlockPos p) {
        return Stairs.of(area).isStair(p);
    }

    public static boolean isBuildingBlock(ItemStack s) {
        return s.is(BUILDING) && s.getItem() instanceof BlockItem;
    }

    static boolean mineable(ServerLevel level, BlockPos p, GolemEntity golem) {
        BlockState s = level.getBlockState(p);
        if (s.isAir() || !s.getFluidState().isEmpty() || s.hasBlockEntity() || s.getDestroySpeed(level, p) < 0) return false;
        if (!golem.canHarvest(s)) return false;
        if (level.getBlockEntity(p.above()) != null) return false;
        for (Direction d : Direction.values()) {
            if (d != Direction.DOWN && !level.getFluidState(p.relative(d)).isEmpty()) return false;
        }
        return true;
    }

    // open or flooded, so a block can go there
    private static boolean open(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return (s.isAir() || s.canBeReplaced()) && !s.hasBlockEntity();
    }

    private static boolean fluid(ServerLevel level, BlockPos p) {
        return !level.getFluidState(p).isEmpty() && level.getBlockState(p).canBeReplaced();
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        Stairs st = Stairs.of(area);
        BlockPos min = area.min(), max = area.max();
        boolean hasBlocks = golem.carries(MinerWork::isBuildingBlock);
        golem.setNeedsBlocks(false);
        // from the rim down: water or lava that got in is sealed first, then missing steps, then the next block to dig
        for (int y = max.getY() + 1; y >= min.getY(); y--) {
            // then the layer below too, so digging never opens on to it
            BlockPos fix = fluidToSeal(golem, level, min, max, y, skip);
            BlockPos step = st.step(st.depth(y));
            if (fix == null && step != null && open(level, step) && !skip.test(step)) fix = step;
            if (fix == null && y > min.getY()) fix = fluidToSeal(golem, level, min, max, y - 1, skip);
            // the next step down too, so a cave under this layer never cuts it off from the rest
            BlockPos below = st.step(st.depth(y - 1));
            if (fix == null && hasBlocks && below != null && y > min.getY() && open(level, below) && !skip.test(below)) fix = below;
            if (fix != null) {
                if (hasBlocks) return fix;
                golem.setNeedsBlocks(true);
                return null;
            }
            if (y > max.getY()) continue;
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(min.getX(), y, min.getZ(), max.getX(), y, max.getZ())) {
                if (st.isStair(p) || skip.test(p) || !mineable(level, p, golem) || underFoot(level, golem, p)) continue;
                double d = golem.distanceToSqr(Vec3.atCenterOf(p));
                if (d < bestDist) {
                    best = p.immutable();
                    bestDist = d;
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    // water or lava in the pit or right at its edge on this layer, ones with somewhere dry to stand first
    private static @Nullable BlockPos fluidToSeal(GolemEntity golem, ServerLevel level, BlockPos min, BlockPos max, int y, Predicate<BlockPos> skip) {
        BlockPos fix = null;
        double fixDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(min.getX() - 1, y, min.getZ() - 1, max.getX() + 1, y, max.getZ() + 1)) {
            if (skip.test(p) || !fluid(level, p) || golem.getBoundingBox().intersects(new AABB(p))) continue;
            double d = golem.distanceToSqr(Vec3.atCenterOf(p)) + (dryStand(level, p) == null ? 10000 : 0);
            if (d < fixDist) {
                fix = p.immutable();
                fixDist = d;
            }
        }
        return fix;
    }

    // the block it stands on, with a drop below it
    private static boolean underFoot(ServerLevel level, GolemEntity golem, BlockPos p) {
        if (!golem.getBoundingBox().move(0, -0.5, 0).intersects(new AABB(p))) return false;
        BlockPos below = p.below();
        return level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    // somewhere dry next to water or lava, so the block does not go where the golem stands
    private static @Nullable BlockPos dryStand(ServerLevel level, BlockPos target) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(target.offset(-2, -1, -2), target.offset(2, 2, 2))) {
            int dx = p.getX() - target.getX(), dz = p.getZ() - target.getZ();
            if (dx == 0 && dz == 0 || dx * dx + dz * dz > 5) continue;
            if (!dry(level, p) || !dry(level, p.above())) continue;
            if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) continue;
            double d = p.distSqr(target);
            if (d < bestDist) {
                best = p.immutable();
                bestDist = d;
            }
        }
        return best;
    }

    private static boolean dry(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getFluidState(p).isEmpty() && !level.getBlockState(p).is(BlockTags.FIRE);
    }

    // the biggest stack of building blocks, up to a full 64, stays with the miner for steps and sealing
    @Override
    public int keep(GolemEntity golem) {
        int best = -1;
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack c = golem.getCarried().get(i);
            if (isBuildingBlock(c) && (best < 0 || c.getCount() > golem.getCarried().get(best).getCount())) best = i;
        }
        return best;
    }

    @Override
    public @Nullable Predicate<ItemStack> restock(GolemEntity golem) {
        return golem.needsBlocks() ? MinerWork::isBuildingBlock : null;
    }

    private static boolean isFix(GolemEntity golem, ServerLevel level, BlockPos pos) {
        Stairs st = Stairs.of(INSTANCE.area(golem));
        return fluid(level, pos) || pos.equals(st.step(st.depth(pos.getY()))) && open(level, pos);
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).isAir();
    }

    @Override
    public boolean isValid(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return isFix(golem, level, pos) ? golem.carries(MinerWork::isBuildingBlock) : isValid(level, pos);
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return open(level, pos) ? 6 : golem.digTicks(level.getBlockState(pos), pos);
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        if (isFix(golem, level, pos)) {
            int slot = keep(golem);
            if (slot < 0 || golem.getBoundingBox().intersects(new AABB(pos))) return;
            ItemStack carried = golem.getCarried().get(slot);
            BlockState state = ((BlockItem) carried.getItem()).getBlock().defaultBlockState();
            level.setBlockAndUpdate(pos, state);
            level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
            carried.shrink(1);
            golem.carriedChanged();
            return;
        }
        ItemStack tool = golem.getTool();
        if (tool.isEmpty() || !mineable(level, pos, golem) || isStair(area(golem), pos)) return;
        BlockState s = level.getBlockState(pos);
        var drops = Block.getDrops(s, level, pos, null, golem, tool);
        LumberWork.breakQuietly(level, pos, s, true);
        tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
        Vec3 at = Vec3.atCenterOf(pos);
        for (ItemStack d : LumberWork.merge(drops)) Storage.collectOrDrop(golem, level, at, d);
    }

    // a missing step is built from the one above it
    @Override
    public @Nullable BlockPos approach(GolemEntity golem, ServerLevel level, BlockPos target) {
        WorkArea area = area(golem);
        Stairs st = Stairs.of(area);
        int d = st.depth(target.getY());
        if (fluid(level, target)) return dryStand(level, target);
        if (!target.equals(st.step(d)) || !open(level, target)) return null;
        BlockPos above = st.step(d - 1);
        return above == null ? new BlockPos(st.minX - 1, st.top + 1, st.minZ) : above.above();
    }

    // a step on its own staircase up to HOP blocks nearer the target's depth
    @Override
    public @Nullable BlockPos waypoint(GolemEntity golem, ServerLevel level, BlockPos target) {
        WorkArea area = area(golem);
        Stairs st = Stairs.of(area);
        if (!area.near(golem.blockPosition(), 2)) return null;
        int here = Mth.clamp(st.top - golem.getBlockY(), 0, st.top - st.bottom);
        int there = Mth.clamp(st.top - target.getY(), 0, st.top - st.bottom);
        int next = here + Mth.clamp(there - here, -HOP, HOP);
        if (next <= 0) return new BlockPos(st.minX - 1, st.top + 1, st.minZ);
        BlockPos s = st.step(next);
        return s == null ? null : s.above();
    }
}
