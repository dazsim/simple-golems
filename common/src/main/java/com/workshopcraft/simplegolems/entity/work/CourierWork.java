package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// takes from the container nearest the first marked corner and fills the one nearest the second
public class CourierWork implements JobWork {
    public static final CourierWork INSTANCE = new CourierWork();

    @Override
    public boolean wants(ItemEntity item) {
        return false;
    }

    public static @Nullable BlockPos source(ServerLevel level, WorkArea area) {
        return Storage.nearest(level, area, Vec3.atCenterOf(area.first()), c -> true, p -> false);
    }

    public static @Nullable BlockPos destination(ServerLevel level, WorkArea area) {
        return Storage.nearest(level, area, Vec3.atCenterOf(area.second()), c -> true, p -> false);
    }

    public static boolean hasRoute(ServerLevel level, WorkArea area) {
        BlockPos from = source(level, area), to = destination(level, area);
        return from != null && to != null && !sameContainer(level, from, to);
    }

    private static int slotToMove(Container from, Container to) {
        for (int i = 0; i < from.getContainerSize(); i++) {
            ItemStack s = from.getItem(i);
            if (!s.isEmpty() && Storage.hasRoom(to, s)) return i;
        }
        return -1;
    }

    private static boolean sameContainer(ServerLevel level, BlockPos a, BlockPos b) {
        Container ca = Storage.containerAt(level, a);
        return a.equals(b) || ca instanceof CompoundContainer cc && level.getBlockEntity(b) instanceof Container half && cc.contains(half);
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        if (!golem.getCarried().isEmpty()) return null;
        BlockPos from = source(level, area), to = destination(level, area);
        if (from == null || to == null || skip.test(from) || sameContainer(level, from, to)) return null;
        Container src = Storage.containerAt(level, from), dst = Storage.containerAt(level, to);
        return src != null && dst != null && slotToMove(src, dst) >= 0 ? from : null;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return Storage.containerAt(level, pos) != null;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 10;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        WorkArea area = golem.getArea();
        BlockPos to = destination(level, area);
        Container src = Storage.containerAt(level, pos), dst = to == null ? null : Storage.containerAt(level, to);
        if (src == null || dst == null || !golem.getCarried().isEmpty()) return;
        int slot = slotToMove(src, dst);
        if (slot < 0) return;
        ItemStack s = src.getItem(slot);
        golem.setCarried(src.removeItem(slot, Math.min(s.getCount(), s.getMaxStackSize())));
        src.setChanged();
        golem.playSound(SoundEvents.BUNDLE_INSERT, 0.6F, 1.2F);
    }

    @Override
    public @Nullable BlockPos dropOff(GolemEntity golem, ServerLevel level, WorkArea area, ItemStack carried, Predicate<BlockPos> skip) {
        BlockPos to = destination(level, area);
        if (to == null || skip.test(to)) return null;
        Container c = Storage.containerAt(level, to);
        return c != null && Storage.hasRoom(c, carried) ? to : null;
    }
}
