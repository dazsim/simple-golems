package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
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

import java.util.function.Predicate;

public final class Storage {
    public static final String WORK_TAG = "simplegolems_work";
    public static final int NEARBY = 2;
    public static final TagKey<Block> EXTRA_STORAGE = TagKey.create(Registries.BLOCK, SimpleGolems.id("storage"));

    private Storage() {
    }

    public static boolean isStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity
                || be.getBlockState().is(EXTRA_STORAGE);
    }

    public static @Nullable Container containerAt(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || !isStorage(be)) return null;
        return HopperBlockEntity.getContainerAt(level, pos);
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
                    if (!area.near(pos, NEARBY) || !isStorage(be) || skip.test(pos)) continue;
                    double d = from.distanceToSqr(Vec3.atCenterOf(pos));
                    if (d >= bestDist) continue;
                    Container c = HopperBlockEntity.getContainerAt(level, pos);
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
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!c.canPlaceItem(i, stack)) continue;
            ItemStack in = c.getItem(i);
            if (in.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < Math.min(in.getMaxStackSize(), c.getMaxStackSize(in))) return true;
        }
        return false;
    }

    public static ItemStack insert(Container c, ItemStack stack) {
        return HopperBlockEntity.addItem(null, c, stack, null);
    }

    public static boolean fits(ItemStack carried, ItemStack stack) {
        if (carried.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(carried, stack) && carried.getCount() < carried.getMaxStackSize();
    }

    public static void pickUp(GolemEntity golem, ItemEntity item) {
        ItemStack carried = golem.getCarried();
        ItemStack stack = item.getItem();
        if (!fits(carried, stack)) return;
        int n = carried.isEmpty() ? Math.min(stack.getCount(), stack.getMaxStackSize()) : Math.min(stack.getCount(), carried.getMaxStackSize() - carried.getCount());
        ItemStack taken = stack.split(n);
        if (carried.isEmpty()) golem.setCarried(taken);
        else {
            carried.grow(n);
            golem.setCarried(carried);
        }
        if (stack.isEmpty()) item.discard();
        else item.setItem(stack);
        golem.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.6F);
    }

    public static void spawnWorkDrop(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity e = new ItemEntity(level, at.x, at.y, at.z, stack);
        e.setDefaultPickUpDelay();
        e.addTag(WORK_TAG);
        level.addFreshEntity(e);
    }
}
