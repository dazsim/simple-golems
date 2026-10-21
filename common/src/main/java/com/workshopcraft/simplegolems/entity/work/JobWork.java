package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface JobWork {
    boolean wants(ItemEntity item);

    @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip);

    boolean isValid(ServerLevel level, BlockPos pos);

    default boolean isValid(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return isValid(level, pos);
    }

    // a spot part of the way to a target that is too far for one path
    default @Nullable BlockPos waypoint(GolemEntity golem, ServerLevel level, BlockPos target) {
        return null;
    }

    // where to stand for a target, when walking at the target itself would go wrong
    default @Nullable BlockPos approach(GolemEntity golem, ServerLevel level, BlockPos target) {
        return null;
    }

    // carry slot kept back from storage, or -1
    default int keep(GolemEntity golem) {
        return -1;
    }

    // the carried stacks that go to storage
    default List<ItemStack> load(GolemEntity golem) {
        int keep = keep(golem);
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack c = golem.getCarried().get(i);
            if (i != keep && !c.isEmpty()) out.add(c);
        }
        return out;
    }

    // what to fetch from storage before work can go on, or null
    default @Nullable Predicate<ItemStack> restock(GolemEntity golem) {
        return null;
    }

    int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos);

    void finish(GolemEntity golem, ServerLevel level, BlockPos pos);

    default WorkArea area(GolemEntity golem) {
        return golem.getArea();
    }

    default @Nullable BlockPos dropOff(GolemEntity golem, ServerLevel level, WorkArea area, ItemStack carried, Predicate<BlockPos> skip) {
        return Storage.nearest(level, area, golem.position(), c -> Storage.hasRoom(c, carried), skip);
    }

    // walk to the ground beside the target rather than onto it
    default boolean fromGround() {
        return false;
    }

    static @Nullable JobWork of(Job job) {
        return switch (job) {
            case LUMBER -> LumberWork.INSTANCE;
            case FARMER -> FarmerWork.INSTANCE;
            case SWEEPER -> SweeperWork.INSTANCE;
            case MINER -> MinerWork.INSTANCE;
            case COURIER -> CourierWork.INSTANCE;
            default -> null;
        };
    }
}
