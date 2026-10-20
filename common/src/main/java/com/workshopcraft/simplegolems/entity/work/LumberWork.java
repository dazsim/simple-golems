package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class LumberWork implements JobWork {
    public static final LumberWork INSTANCE = new LumberWork();
    private static final int MAX_LOGS = 256;
    private static final int MAX_REACH = 12;

    record Tree(List<BlockPos> logs, List<BlockPos> leaves, List<BlockPos> bases) {
    }

    @Override
    public boolean wants(ItemEntity item) {
        ItemStack s = item.getItem();
        return item.entityTags().contains(Storage.WORK_TAG) || s.is(ItemTags.SAPLINGS) || s.is(ItemTags.LOGS)
                || s.is(Items.STICK) || s.is(Items.APPLE);
    }

    private static boolean isBase(ServerLevel level, BlockPos pos, BlockState state) {
        return state.is(BlockTags.LOGS) && level.getBlockState(pos.below()).is(BlockTags.DIRT);
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        List<BlockPos> bases = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(area.min(), area.max())) {
            if (isBase(level, p, level.getBlockState(p)) && !skip.test(p)) bases.add(p.immutable());
        }
        bases.sort(Comparator.comparingDouble(p -> golem.distanceToSqr(Vec3.atCenterOf(p))));
        for (BlockPos p : bases) {
            if (tree(level, p) != null) return p;
        }
        return null;
    }

    // a grown tree has natural leaves touching its logs; log cabins and stripped builds are left alone
    static @Nullable Tree tree(ServerLevel level, BlockPos base) {
        if (!level.getBlockState(base).is(BlockTags.LOGS)) return null;
        Block kind = level.getBlockState(base).getBlock();
        Set<BlockPos> logs = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        logs.add(base);
        queue.add(base);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
                if (logs.contains(n) || Math.abs(n.getX() - base.getX()) > MAX_REACH || Math.abs(n.getZ() - base.getZ()) > MAX_REACH) continue;
                BlockState s = level.getBlockState(n);
                if (!s.is(kind)) continue;
                BlockPos imm = n.immutable();
                logs.add(imm);
                queue.add(imm);
                if (logs.size() > MAX_LOGS) return null;
            }
        }
        Map<BlockPos, Integer> depth = new HashMap<>();
        for (BlockPos log : logs) {
            for (Direction d : Direction.values()) {
                BlockPos n = log.relative(d);
                if (!logs.contains(n) && ownedLeaf(level, n, 1)) {
                    depth.putIfAbsent(n, 1);
                }
            }
        }
        if (depth.isEmpty()) return null;
        ArrayDeque<BlockPos> leafQueue = new ArrayDeque<>(depth.keySet());
        while (!leafQueue.isEmpty()) {
            BlockPos p = leafQueue.poll();
            int dp = depth.get(p);
            if (dp >= LeavesBlock.DECAY_DISTANCE - 1) continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (depth.containsKey(n) || logs.contains(n)) continue;
                if (ownedLeaf(level, n, dp + 1)) {
                    depth.put(n, dp + 1);
                    leafQueue.add(n);
                }
            }
        }
        List<BlockPos> bases = new ArrayList<>();
        for (BlockPos log : logs) {
            if (level.getBlockState(log.below()).is(BlockTags.DIRT)) bases.add(log);
        }
        List<BlockPos> sorted = new ArrayList<>(logs);
        sorted.sort(Comparator.comparingInt((BlockPos p) -> -p.getY()));
        return new Tree(sorted, new ArrayList<>(depth.keySet()), bases);
    }

    // natural leaves whose nearest log is this far away, so leaves kept alive by a neighbouring tree stay
    private static boolean ownedLeaf(ServerLevel level, BlockPos pos, int dist) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof LeavesBlock && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT)
                && s.getValue(LeavesBlock.DISTANCE) == dist;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.LOGS);
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 40;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        Tree tree = tree(level, pos);
        if (tree == null) return;
        List<ItemStack> drops = new ArrayList<>();
        List<BlockPos> cleared = new ArrayList<>();
        int particles = 0;
        for (BlockPos log : tree.logs()) {
            ItemStack tool = golem.getTool();
            if (tool.isEmpty()) break;
            BlockState s = level.getBlockState(log);
            drops.addAll(Block.getDrops(s, level, log, level.getBlockEntity(log), golem, tool));
            breakQuietly(level, log, s, particles++ < 12);
            cleared.add(log);
            tool.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
        }
        if (cleared.size() == tree.logs().size()) {
            for (BlockPos leaf : tree.leaves()) {
                BlockState s = level.getBlockState(leaf);
                drops.addAll(Block.getDrops(s, level, leaf, null, golem, ItemStack.EMPTY));
                breakQuietly(level, leaf, s, particles++ < 24);
            }
        }
        for (BlockPos base : tree.bases()) {
            if (!cleared.contains(base) || !level.getBlockState(base).isAir()) continue;
            for (ItemStack d : drops) {
                if (d.isEmpty() || !d.is(ItemTags.SAPLINGS) || !(d.getItem() instanceof BlockItem bi)) continue;
                BlockState sapling = bi.getBlock().defaultBlockState();
                if (sapling.canSurvive(level, base)) {
                    level.setBlockAndUpdate(base, sapling);
                    d.shrink(1);
                    break;
                }
            }
        }
        Vec3 at = Vec3.atBottomCenterOf(pos).add(0, 0.5, 0);
        for (ItemStack d : merge(drops)) Storage.spawnWorkDrop(level, at, d);
    }

    static void breakQuietly(ServerLevel level, BlockPos pos, BlockState state, boolean effects) {
        if (effects) level.levelEvent(2001, pos, Block.getId(state));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    static List<ItemStack> merge(List<ItemStack> drops) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack d : drops) {
            if (d.isEmpty()) continue;
            ItemStack left = d.copy();
            for (ItemStack o : out) {
                if (ItemStack.isSameItemSameComponents(o, left) && o.getCount() < o.getMaxStackSize()) {
                    int n = Math.min(left.getCount(), o.getMaxStackSize() - o.getCount());
                    o.grow(n);
                    left.shrink(n);
                    if (left.isEmpty()) break;
                }
            }
            if (!left.isEmpty()) out.add(left);
        }
        return out;
    }
}
