package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

// harvests full hives and nests in its area: a honey bottle when it has glass bottles, honeycomb with shears; the bees stay calm
public class BeekeeperWork implements JobWork {
    public static final BeekeeperWork INSTANCE = new BeekeeperWork();

    @Override
    public boolean wants(ItemEntity item) {
        return item.getItem().is(Items.HONEYCOMB) || item.getItem().is(Items.HONEY_BOTTLE);
    }

    @Override
    public boolean keeps(GolemEntity golem, int slot) {
        return golem.getCarried().get(slot).is(Items.GLASS_BOTTLE);
    }

    @Override
    public String supplies() {
        return "bottles";
    }

    private static boolean full(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getBlock() instanceof BeehiveBlock && s.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS;
    }

    private static List<BlockPos> fullHives(ServerLevel level, WorkArea area) {
        List<BlockPos> out = new ArrayList<>();
        for (int cx = area.min().getX() >> 4; cx <= area.max().getX() >> 4; cx++) {
            for (int cz = area.min().getZ() >> 4; cz <= area.max().getZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof BeehiveBlockEntity && area.contains(be.getBlockPos()) && full(level, be.getBlockPos())) out.add(be.getBlockPos());
                }
            }
        }
        return out;
    }

    private static int bottles(GolemEntity golem) {
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) if (golem.getCarried().get(i).is(Items.GLASS_BOTTLE)) return i;
        return -1;
    }

    private static boolean equipped(GolemEntity golem) {
        return bottles(golem) >= 0 || golem.getTool().is(Items.SHEARS);
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        if (!equipped(golem)) return null;
        golem.setSupplies(null);
        return fullHives(level, area).stream().filter(p -> !skip.test(p)).min(Comparator.comparingDouble(p -> golem.distanceToSqr(Vec3.atCenterOf(p)))).orElse(null);
    }

    @Override
    public @Nullable Predicate<ItemStack> restock(GolemEntity golem) {
        if (equipped(golem) || !(golem.level() instanceof ServerLevel level) || fullHives(level, area(golem)).isEmpty()) {
            golem.setSupplies(null);
            return null;
        }
        return s -> s.is(Items.GLASS_BOTTLE);
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return full(level, pos);
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 20;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        if (!full(level, pos)) return;
        BlockState s = level.getBlockState(pos);
        int b = bottles(golem);
        if (b >= 0) {
            golem.getCarried().get(b).shrink(1);
            golem.carriedChanged();
            Storage.collectOrDrop(golem, level, Vec3.atCenterOf(pos), new ItemStack(Items.HONEY_BOTTLE));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (golem.getTool().is(Items.SHEARS)) {
            BeehiveBlock.dropHoneycomb(level, golem.getTool(), s, level.getBlockEntity(pos), golem, pos);
            golem.getTool().hurtAndBreak(1, golem, EquipmentSlot.MAINHAND);
            level.playSound(null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            return;
        }
        // no bees are let out or angered, as if the hive were smoked
        ((BeehiveBlock) s.getBlock()).resetHoneyLevel(level, s, pos);
    }
}
