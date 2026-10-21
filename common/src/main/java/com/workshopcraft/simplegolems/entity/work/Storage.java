package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class Storage {
    public static final String WORK_TAG = "simplegolems_work";
    public static final int NEARBY = 2;
    public static final TagKey<Block> EXTRA_STORAGE = TagKey.create(Registries.BLOCK, SimpleGolems.id("storage"));
    public static final TagKey<Block> NOT_STORAGE = TagKey.create(Registries.BLOCK, SimpleGolems.id("not_storage"));
    // set by each loader: another mod's item storage at a block, through the loader's own item API
    public static BiFunction<ServerLevel, BlockPos, @Nullable Container> external = (level, pos) -> null;

    private Storage() {
    }

    public static boolean isStorage(BlockEntity be) {
        if (be.getBlockState().is(NOT_STORAGE)) return false;
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity
                || be.getBlockState().is(EXTRA_STORAGE);
    }

    // vanilla chests, barrels and shulker boxes, blocks in the storage tag, and other mods' blocks with an item inventory
    public static @Nullable Container containerAt(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        if (isStorage(be)) {
            Container c = HopperBlockEntity.getContainerAt(level, pos);
            return c != null ? c : Config.moddedStorage ? external.apply(level, pos) : null;
        }
        if (!Config.moddedStorage || be.getBlockState().is(NOT_STORAGE) || BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getNamespace().equals("minecraft")) return null;
        return external.apply(level, pos);
    }

    // nearest storage in or just around the area to 'from' that matches the test
    public static @Nullable BlockPos nearest(ServerLevel level, WorkArea area, Vec3 from, Predicate<Container> test, Predicate<BlockPos> skip) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cx = (area.min().getX() - NEARBY) >> 4; cx <= (area.max().getX() + NEARBY) >> 4; cx++) {
            for (int cz = (area.min().getZ() - NEARBY) >> 4; cz <= (area.max().getZ() + NEARBY) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (!area.near(pos, NEARBY) || skip.test(pos)) continue;
                    double d = from.distanceToSqr(Vec3.atCenterOf(pos));
                    if (d >= bestDist) continue;
                    Container c = containerAt(level, pos);
                    if (c != null && test.test(c)) {
                        best = pos.immutable();
                        bestDist = d;
                    }
                }
            }
        }
        return best;
    }

    public static boolean hasRoom(Container c, ItemStack stack) {
        if (c instanceof ExternalStore e) return e.put(stack, true) > 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!c.canPlaceItem(i, stack)) continue;
            ItemStack in = c.getItem(i);
            if (in.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < Math.min(in.getMaxStackSize(), c.getMaxStackSize(in))) return true;
        }
        return false;
    }

    public static ItemStack insert(Container c, ItemStack stack) {
        if (c instanceof ExternalStore e) {
            if (stack.isEmpty()) return stack;
            int in = e.put(stack, false);
            return stack.copyWithCount(stack.getCount() - in);
        }
        return HopperBlockEntity.addItem(null, c, stack, null);
    }

    public static void pickUp(GolemEntity golem, ItemEntity item) {
        ItemStack stack = item.getItem();
        if (!golem.canCarry(stack)) return;
        golem.addCarried(stack);
        if (stack.isEmpty()) item.discard();
        else item.setItem(stack);
        golem.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.6F);
    }

    // what fits the carry slots goes straight in, the rest is dropped for later trips
    public static void collectOrDrop(GolemEntity golem, ServerLevel level, Vec3 at, ItemStack stack) {
        golem.addCarried(stack);
        spawnWorkDrop(level, at, stack);
    }

    public static void spawnWorkDrop(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity e = new ItemEntity(level, at.x, at.y, at.z, stack);
        e.setDefaultPickUpDelay();
        e.addTag(WORK_TAG);
        level.addFreshEntity(e);
    }
}
