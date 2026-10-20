package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public interface JobWork {
    boolean wants(ItemEntity item);

    @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip);

    boolean isValid(ServerLevel level, BlockPos pos);

    int workTicks(ServerLevel level, BlockPos pos);

    void finish(GolemEntity golem, ServerLevel level, BlockPos pos);

    static @Nullable JobWork of(Job job) {
        return switch (job) {
            case LUMBER -> LumberWork.INSTANCE;
            case FARMER -> FarmerWork.INSTANCE;
            case SWEEPER -> SweeperWork.INSTANCE;
            default -> null;
        };
    }
}
