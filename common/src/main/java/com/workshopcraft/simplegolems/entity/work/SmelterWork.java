package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

// clicks: input chest, fuel chest, furnace (or smoker or blast furnace), output chest
public class SmelterWork extends StationWork {
    public static final SmelterWork INSTANCE = new SmelterWork();
    private static final int IN = 0, FUEL = 1, OUT = 2;
    // keeps a few fuel in the furnace rather than filling it
    private static final int FUEL_KEEP = 8;

    @Override
    protected int inputs() {
        return 2;
    }

    @Override
    protected int output() {
        return 3;
    }

    private static @Nullable AbstractFurnaceBlockEntity furnace(GolemEntity golem, ServerLevel level) {
        BlockPos p = point(golem, 2);
        return p != null && level.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity f ? f : null;
    }

    private static Predicate<ItemStack> smeltable(GolemEntity golem, ServerLevel level, AbstractFurnaceBlockEntity f) {
        var type = f instanceof BlastFurnaceBlockEntity ? RecipeType.BLASTING : f instanceof SmokerBlockEntity ? RecipeType.SMOKING : RecipeType.SMELTING;
        // with one chest for both, burnable things such as logs count as fuel
        boolean shared = point(golem, 0) != null && point(golem, 0).equals(point(golem, 1));
        return s -> !s.isEmpty() && (!shared || !level.fuelValues().isFuel(s))
                && level.recipeAccess().getRecipeFor(type, new SingleRecipeInput(s), level).isPresent();
    }

    private static Predicate<ItemStack> fuel(ServerLevel level) {
        return s -> !s.isEmpty() && level.fuelValues().isFuel(s);
    }

    private static boolean fitsSlot(AbstractFurnaceBlockEntity f, int slot, ItemStack s) {
        ItemStack in = f.getItem(slot);
        return !s.isEmpty() && (in.isEmpty() || ItemStack.isSameItemSameComponents(in, s) && in.getCount() < in.getMaxStackSize());
    }

    // the empty bucket a lava bucket leaves behind
    private static boolean spentFuel(ServerLevel level, AbstractFurnaceBlockEntity f) {
        ItemStack s = f.getItem(FUEL);
        return !s.isEmpty() && !level.fuelValues().isFuel(s);
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        AbstractFurnaceBlockEntity f = furnace(golem, level);
        golem.setSupplies(f == null ? "furnace" : null);
        if (f == null) return null;
        BlockPos at = f.getBlockPos();
        boolean room = room(golem);
        if (room && (!f.getItem(OUT).isEmpty() || spentFuel(level, f))) return at;
        Predicate<ItemStack> smelt = smeltable(golem, level, f);
        if (smelt.test(input(golem, 0)) && fitsSlot(f, IN, input(golem, 0))) return at;
        boolean lowFuel = f.getItem(FUEL).getCount() < FUEL_KEEP;
        if (lowFuel && fuel(level).test(input(golem, 1)) && fitsSlot(f, FUEL, input(golem, 1))) return at;
        BlockPos inChest = point(golem, 0), fuelChest = point(golem, 1);
        if (input(golem, 0).isEmpty() && (f.getItem(IN).isEmpty() || f.getItem(IN).getCount() < f.getItem(IN).getMaxStackSize())
                && sourceHas(level, inChest, s -> smelt.test(s) && fitsSlot(f, IN, s)) && !skip.test(inChest)) return inChest;
        boolean needsFuel = lowFuel && (!f.getItem(IN).isEmpty() || !input(golem, 0).isEmpty());
        if (needsFuel && input(golem, 1).isEmpty()) {
            if (sourceHas(level, fuelChest, fuel(level)) && !skip.test(fuelChest)) return fuelChest;
            if (f.getItem(FUEL).isEmpty() && !f.getBlockState().getValue(AbstractFurnaceBlock.LIT)) golem.setSupplies("fuel");
        }
        return null;
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 8;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        AbstractFurnaceBlockEntity f = furnace(golem, level);
        if (f == null) return;
        if (!pos.equals(f.getBlockPos())) {
            Predicate<ItemStack> smelt = smeltable(golem, level, f);
            boolean took = false;
            if (pos.equals(point(golem, 0)) && input(golem, 0).isEmpty()) took = take(golem, level, pos, 0, s -> smelt.test(s) && fitsSlot(f, IN, s));
            if (!took && pos.equals(point(golem, 1)) && input(golem, 1).isEmpty()) take(golem, level, pos, 1, fuel(level));
            return;
        }
        Vec3 at = Vec3.atCenterOf(pos);
        ItemStack out = f.getItem(OUT);
        if (!out.isEmpty() && room(golem)) {
            int n = Math.min(out.getCount(), fits(golem, out));
            addResult(golem, level, f.removeItem(OUT, n), at);
        }
        if (spentFuel(level, f) && room(golem)) addResult(golem, level, f.removeItem(FUEL, 64), at);
        put(f, IN, input(golem, 0), smeltable(golem, level, f));
        if (f.getItem(FUEL).getCount() < FUEL_KEEP) put(f, FUEL, input(golem, 1), fuel(level));
        f.setChanged();
        golem.carriedChanged();
        golem.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.6F, 1.3F);
    }

    private static void put(AbstractFurnaceBlockEntity f, int slot, ItemStack from, Predicate<ItemStack> ok) {
        if (!ok.test(from) || !fitsSlot(f, slot, from)) return;
        ItemStack in = f.getItem(slot);
        if (in.isEmpty()) {
            f.setItem(slot, from.split(from.getMaxStackSize()));
        } else {
            int n = Math.min(from.getCount(), in.getMaxStackSize() - in.getCount());
            in.grow(n);
            from.shrink(n);
        }
    }
}
