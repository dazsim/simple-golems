package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class FarmerWork implements JobWork {
    public static final FarmerWork INSTANCE = new FarmerWork();

    @Override
    public boolean wants(ItemEntity item) {
        return item.entityTags().contains(Storage.WORK_TAG);
    }

    // CropBlock covers vanilla and most modded crops; anything else in #crops with a plain age property also counts
    static @Nullable IntegerProperty age(BlockState state) {
        if (state.getBlock() instanceof StemBlock || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return null;
        if (!(state.getBlock() instanceof CropBlock) && !state.is(BlockTags.CROPS)) return null;
        for (Property<?> p : state.getProperties()) {
            if (p instanceof IntegerProperty ip && p.getName().equals("age")) return ip;
        }
        return null;
    }

    static boolean ripe(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        IntegerProperty age = age(state);
        return age != null && state.getValue(age) >= java.util.Collections.max(age.getPossibleValues());
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(area.min(), area.max())) {
            BlockState s = level.getBlockState(p);
            if (s.isAir() || !ripe(s) || skip.test(p)) continue;
            double d = golem.distanceToSqr(Vec3.atCenterOf(p));
            if (d < bestDist) {
                best = p.immutable();
                bestDist = d;
            }
        }
        return best;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return ripe(level.getBlockState(pos));
    }

    @Override
    public int workTicks(ServerLevel level, BlockPos pos) {
        return 10;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!ripe(state)) return;
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), golem, golem.getTool());
        Item seed = state.getBlock().asItem();
        for (ItemStack d : drops) {
            if (d.is(seed)) {
                d.shrink(1);
                break;
            }
        }
        BlockState replant = state.getBlock() instanceof CropBlock crop ? crop.getStateForAge(0) : state.setValue(age(state), java.util.Collections.min(age(state).getPossibleValues()));
        level.levelEvent(2001, pos, Block.getId(state));
        level.setBlockAndUpdate(pos, replant);
        golem.getTool().hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
        Vec3 at = Vec3.atBottomCenterOf(pos).add(0, 0.3, 0);
        for (ItemStack d : LumberWork.merge(drops)) Storage.spawnWorkDrop(level, at, d);
    }
}
