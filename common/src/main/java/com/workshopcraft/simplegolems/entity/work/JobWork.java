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

    // carry slots that never go to storage
    default boolean keeps(GolemEntity golem, int slot) {
        return false;
    }

    // lang key part for what a missing restock is, shown in the status and as the supplies bubble
    default @Nullable String supplies() {
        return null;
    }

    // how many of the kept slot stay back
    default int reserve(GolemEntity golem) {
        return 0;
    }

    // the carried stacks that go to storage
    default List<ItemStack> load(GolemEntity golem) {
        int keep = keep(golem);
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack c = golem.getCarried().get(i);
            if (!c.isEmpty() && !keeps(golem, i) && (i != keep || c.getCount() > reserve(golem))) out.add(c);
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

    // blocks it may build steps with to reach a target above it, or null if it never builds up
    default @Nullable Predicate<ItemStack> climbWith(GolemEntity golem) {
        return null;
    }

    // lang key part for the status when it has none of those blocks
    default String climbSupplies() {
        return "climb_blocks";
    }

    // what it may break when it is in the way of those steps
    default boolean climbClears(ServerLevel level, BlockPos pos) {
        return false;
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
            case COBBLEGEN -> CobblegenWork.INSTANCE;
            case SIFTER -> SifterWork.INSTANCE;
            case HAMMER -> HammerWork.INSTANCE;
            case SHEPHERD -> ShepherdWork.INSTANCE;
            case RANCHER -> RancherWork.INSTANCE;
            case COMPOSTER -> ComposterWork.INSTANCE;
            case SMELTER -> SmelterWork.INSTANCE;
            case BEEKEEPER -> BeekeeperWork.INSTANCE;
            case FISHER -> FisherWork.INSTANCE;
            case LAMPLIGHTER -> LamplighterWork.INSTANCE;
            default -> null;
        };
    }
}
