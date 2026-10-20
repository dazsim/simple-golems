package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public interface JobWork {
    boolean wants(ItemEntity item);

    @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip);

    boolean isValid(ServerLevel level, BlockPos pos);

    int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos);

    void finish(GolemEntity golem, ServerLevel level, BlockPos pos);

    default WorkArea area(GolemEntity golem) {
        return golem.getArea();
    }

    default @Nullable BlockPos dropOff(GolemEntity golem, ServerLevel level, WorkArea area, ItemStack carried, Predicate<BlockPos> skip) {
        return Storage.nearest(level, area, golem.position(), c -> Storage.hasRoom(c, carried), skip);
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
