package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Predicate;

// fishes from the bank of water in its area, using the fishing loot table; the catch goes to storage
public class FisherWork implements JobWork {
    public static final FisherWork INSTANCE = new FisherWork();
    private static final int CAST = 300;

    @Override
    public boolean wants(ItemEntity item) {
        return item.entityTags().contains(Storage.WORK_TAG);
    }

    private static boolean water(ServerLevel level, BlockPos p) {
        return level.getFluidState(p).is(FluidTags.WATER) && level.getFluidState(p).isSource() && level.getBlockState(p.above()).isAir();
    }

    // dry ground beside the water to stand on
    private static @Nullable BlockPos bank(ServerLevel level, BlockPos w) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos p = w.relative(d).above(dy), below = p.below();
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getFluidState(p).isEmpty()
                        && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                        && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return p;
            }
        }
        return null;
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(area.min(), area.max())) {
            if (!water(level, p) || skip.test(p)) continue;
            double d = golem.distanceToSqr(Vec3.atCenterOf(p));
            if (d < bd && bank(level, p) != null) {
                bd = d;
                best = p.immutable();
            }
        }
        return best;
    }

    @Override
    public @Nullable BlockPos approach(GolemEntity golem, ServerLevel level, BlockPos target) {
        return bank(level, target);
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return water(level, pos);
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return CAST;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        ItemStack rod = golem.getTool();
        if (rod.isEmpty() || !water(level, pos)) return;
        Vec3 at = Vec3.atCenterOf(pos).add(0, 0.5, 0);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, at).withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, golem).create(LootContextParamSets.FISHING);
        for (ItemStack s : level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params)) {
            Storage.collectOrDrop(golem, level, golem.position(), s);
        }
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 12, 0.3, 0.0, 0.3, 0.1);
        level.playSound(null, pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5F, 1.0F);
        rod.hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
    }
}
