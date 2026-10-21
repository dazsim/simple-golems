package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.registry.ModTags;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
        return item.entityTags().contains(Storage.WORK_TAG) || s.is(ModTags.SAPLINGS) || s.is(ModTags.LOG_ITEMS)
                || s.is(Items.STICK) || s.is(Items.APPLE);
    }

    private static boolean isBase(ServerLevel level, BlockPos pos, BlockState state) {
        return state.is(ModTags.LOGS) && level.getBlockState(pos.below()).is(BlockTags.SUBSTRATE_OVERWORLD);
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        List<BlockPos> bases = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(area.min(), area.max())) {
            BlockState s = level.getBlockState(p);
            if (isBase(level, p, s) && golem.canHarvest(s) && !skip.test(p)) bases.add(p.immutable());
        }
        bases.sort(Comparator.comparingDouble(p -> golem.distanceToSqr(Vec3.atCenterOf(p))));
        for (BlockPos p : bases) {
            if (tree(level, p) != null) return p;
        }
        return null;
    }

    // a grown tree has natural leaves touching its logs; log cabins and stripped builds are left alone
    static @Nullable Tree tree(ServerLevel level, BlockPos base) {
        if (!level.getBlockState(base).is(ModTags.LOGS)) return null;
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
            if (level.getBlockState(log.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) bases.add(log);
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

    // the stump of the natural tree whose trunk or branch this log belongs to
    public static @Nullable BlockPos baseUnder(ServerLevel level, BlockPos log) {
        if (!level.getBlockState(log).is(ModTags.LOGS)) return null;
        for (int y = 1; y <= 24; y++) {
            for (BlockPos p : BlockPos.betweenClosed(log.offset(-3, -y, -3), log.offset(3, -y, 3))) {
                BlockState s = level.getBlockState(p);
                if (!isBase(level, p, s)) continue;
                Tree t = tree(level, p.immutable());
                if (t != null && t.logs().contains(log)) return p.immutable();
            }
        }
        return null;
    }

    public static boolean isTreeLeaf(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof LeavesBlock && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT);
    }

    public static void breakLeaf(GolemEntity golem, ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(s, level, pos, null, golem, golem.getTool());
        breakQuietly(level, pos, s, true);
        for (ItemStack d : merge(drops)) Storage.collectOrDrop(golem, level, Vec3.atCenterOf(pos), d);
    }

    @Override
    public boolean fromGround() {
        return true;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.LOGS);
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        Tree tree = tree(level, pos);
        int logs = tree == null ? 1 : tree.logs().size();
        return Math.min(200, golem.digTicks(level.getBlockState(pos), pos) * (1 + logs / 4));
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
                drops.addAll(Block.getDrops(s, level, leaf, null, golem, golem.getTool()));
                breakQuietly(level, leaf, s, particles++ < 24);
            }
        }
        for (BlockPos base : tree.bases()) {
            if (!cleared.contains(base) || !level.getBlockState(base).isAir()) continue;
            golem.setSaplingTrouble(!replant(golem, level, base, drops));
        }
        Vec3 at = Vec3.atBottomCenterOf(pos).add(0, 0.5, 0);
        for (ItemStack d : merge(drops)) Storage.collectOrDrop(golem, level, at, d);
    }

    private static boolean replant(GolemEntity golem, ServerLevel level, BlockPos base, List<ItemStack> drops) {
        if (plant(level, base, drops)) return true;
        List<ItemEntity> near = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(3), e -> e.isAlive() && e.getItem().is(ModTags.SAPLINGS));
        if (near.stream().anyMatch(e -> plantFrom(level, base, e))) return true;
        if (plant(level, base, golem.getCarried())) {
            golem.carriedChanged();
            return true;
        }
        // nothing dropped, so borrow a sapling from storage in the area
        BlockPos chest = Storage.nearest(level, golem.getArea(), golem.position(), c -> saplingSlot(c, level, base) >= 0, p -> false);
        Container c = chest == null ? null : Storage.containerAt(level, chest);
        if (c == null) return false;
        int slot = saplingSlot(c, level, base);
        if (slot < 0) return false;
        ItemStack one = c.removeItem(slot, 1);
        plant(level, base, List.of(one));
        if (!one.isEmpty()) Storage.insert(c, one);
        c.setChanged();
        return true;
    }

    private static @Nullable BlockState saplingFor(ItemStack stack, ServerLevel level, BlockPos pos) {
        if (stack.isEmpty() || !stack.is(ModTags.SAPLINGS) || !(stack.getItem() instanceof BlockItem bi)) return null;
        BlockState sapling = bi.getBlock().defaultBlockState();
        return sapling.canSurvive(level, pos) ? sapling : null;
    }

    private static boolean plant(ServerLevel level, BlockPos pos, List<ItemStack> stacks) {
        for (ItemStack s : stacks) {
            BlockState sapling = saplingFor(s, level, pos);
            if (sapling != null) {
                level.setBlockAndUpdate(pos, sapling);
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean plantFrom(ServerLevel level, BlockPos pos, ItemEntity e) {
        ItemStack s = e.getItem().copy();
        if (!plant(level, pos, List.of(s))) return false;
        if (s.isEmpty()) e.discard();
        else e.setItem(s);
        return true;
    }

    private static int saplingSlot(Container c, ServerLevel level, BlockPos pos) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (saplingFor(c.getItem(i), level, pos) != null) return i;
        }
        return -1;
    }

    public static void breakQuietly(ServerLevel level, BlockPos pos, BlockState state, boolean effects) {
        if (effects) level.levelEvent(2001, pos, Block.getId(state));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static List<ItemStack> merge(List<ItemStack> drops) {
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
