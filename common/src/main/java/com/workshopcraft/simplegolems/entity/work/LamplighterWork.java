package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// puts torches and lamps (the lamplighter_lights item tag) from storage on dark ground in its area, where monsters could spawn
public class LamplighterWork implements JobWork {
    public static final LamplighterWork INSTANCE = new LamplighterWork();

    @Override
    public boolean wants(ItemEntity item) {
        return false;
    }

    @Override
    public boolean keeps(GolemEntity golem, int slot) {
        return golem.getCarried().get(slot).is(ModTags.LIGHTS);
    }

    @Override
    public String supplies() {
        return "torches";
    }

    private static int light(GolemEntity golem) {
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack s = golem.getCarried().get(i);
            if (s.is(ModTags.LIGHTS) && s.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    // dark ground with room above it, where a monster could stand; not a gap under the floor
    private static boolean dark(ServerLevel level, BlockPos p, BlockState torch) {
        if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir() || level.getBrightness(LightLayer.BLOCK, p) > 0) return false;
        BlockPos below = p.below();
        BlockState floor = level.getBlockState(below);
        return floor.isFaceSturdy(level, below, Direction.UP) && floor.getFluidState().isEmpty() && !floor.hasBlockEntity() && torch.canSurvive(level, p);
    }

    private static @Nullable BlockPos darkest(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip, BlockState torch) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(area.min(), area.max())) {
            if (skip.test(p) || !dark(level, p, torch)) continue;
            double d = golem.distanceToSqr(Vec3.atBottomCenterOf(p));
            if (d < bd) {
                bd = d;
                best = p.immutable();
            }
        }
        return best;
    }

    private static BlockState torch(GolemEntity golem) {
        int i = light(golem);
        return i >= 0 ? ((BlockItem) golem.getCarried().get(i).getItem()).getBlock().defaultBlockState()
                : net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState();
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        if (light(golem) < 0) return null;
        golem.setSupplies(null);
        return darkest(golem, level, area, skip, torch(golem));
    }

    @Override
    public @Nullable Predicate<ItemStack> restock(GolemEntity golem) {
        if (light(golem) >= 0 || !(golem.level() instanceof ServerLevel level) || darkest(golem, level, area(golem), p -> false, torch(golem)) == null) {
            golem.setSupplies(null);
            return null;
        }
        return s -> s.is(ModTags.LIGHTS) && s.getItem() instanceof BlockItem;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBrightness(LightLayer.BLOCK, pos) == 0;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 6;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        int i = light(golem);
        if (i < 0) return;
        if (!dark(level, pos, torch(golem))) return;
        // placed the way a player would, so lamps from other mods come out lit and whole
        ItemStack stack = golem.getCarried().get(i);
        if (((BlockItem) stack.getItem()).place(new DirectionalPlaceContext(level, pos, Direction.DOWN, stack, Direction.UP)).consumesAction()) {
            golem.carriedChanged();
        }
    }
}
