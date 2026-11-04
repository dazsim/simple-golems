package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// programs that work between blocks clicked with the marker stick: the first carry slots hold what it works with, the rest what it makes
public abstract class StationWork implements JobWork {
    // carry slots below this hold inputs, the others results
    protected int inputs() {
        return 1;
    }

    // the clicked block the results go to
    protected abstract int output();

    public static @Nullable BlockPos point(GolemEntity golem, int i) {
        List<BlockPos> p = golem.getPoints();
        return i < p.size() ? p.get(i) : null;
    }

    @Override
    public boolean wants(ItemEntity item) {
        return false;
    }

    @Override
    public boolean keeps(GolemEntity golem, int slot) {
        return slot < inputs();
    }

    @Override
    public List<ItemStack> load(GolemEntity golem) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack c = golem.getCarried().get(i);
            if (!c.isEmpty()) out.add(c);
        }
        return out;
    }

    @Override
    public @Nullable BlockPos dropOff(GolemEntity golem, ServerLevel level, WorkArea area, ItemStack carried, Predicate<BlockPos> skip) {
        BlockPos to = point(golem, output());
        if (to == null || skip.test(to)) return null;
        Container c = Storage.containerAt(level, to);
        return c != null && Storage.hasRoom(c, carried) ? to : null;
    }

    protected boolean room(GolemEntity golem) {
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS; i++) if (golem.getCarried().get(i).isEmpty()) return true;
        return false;
    }

    protected ItemStack input(GolemEntity golem, int slot) {
        return golem.getCarried().get(slot);
    }

    // what does not fit stays on the ground for the next turn
    protected void addResult(GolemEntity golem, ServerLevel level, ItemStack stack, Vec3 at) {
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS && !stack.isEmpty(); i++) {
            ItemStack c = golem.getCarried().get(i);
            if (!c.isEmpty() && ItemStack.isSameItemSameComponents(c, stack)) {
                int n = Math.min(stack.getCount(), c.getMaxStackSize() - c.getCount());
                c.grow(n);
                stack.shrink(n);
            }
        }
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS && !stack.isEmpty(); i++) {
            if (golem.getCarried().get(i).isEmpty()) {
                golem.getCarried().set(i, stack.split(stack.getMaxStackSize()));
            }
        }
        golem.carriedChanged();
        Storage.spawnWorkDrop(level, at, stack);
    }

    // how many of a stack the result slots can take
    protected int fits(GolemEntity golem, ItemStack stack) {
        int n = 0;
        for (int i = inputs(); i < GolemEntity.CARRY_SLOTS; i++) {
            ItemStack c = golem.getCarried().get(i);
            if (c.isEmpty()) n += stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(c, stack)) n += c.getMaxStackSize() - c.getCount();
        }
        return n;
    }

    // results lying on or just above a block, such as a sieve's
    protected void collect(GolemEntity golem, ServerLevel level, BlockPos pos) {
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0, 1.5, 1.0), ItemEntity::isAlive)) {
            ItemStack s = e.getItem();
            int n = Math.min(s.getCount(), fits(golem, s));
            if (n <= 0) continue;
            addResult(golem, level, s.split(n), e.position());
            if (s.isEmpty()) e.discard();
            else e.setItem(s);
        }
    }

    public static boolean has(Container c, Predicate<ItemStack> want) {
        for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty() && want.test(c.getItem(i))) return true;
        return false;
    }

    // a stack of the first kind of item that passes the test, into an input slot
    protected boolean take(GolemEntity golem, ServerLevel level, BlockPos from, int slot, Predicate<ItemStack> want) {
        Container c = Storage.containerAt(level, from);
        if (c == null) return false;
        ItemStack got = golem.getCarried().get(slot);
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty() || !want.test(s) || !got.isEmpty() && !ItemStack.isSameItemSameComponents(got, s)) continue;
            int room = got.isEmpty() ? s.getMaxStackSize() : got.getMaxStackSize() - got.getCount();
            if (room <= 0) break;
            ItemStack t = c.removeItem(i, Math.min(room, s.getCount()));
            if (got.isEmpty()) got = t;
            else got.grow(t.getCount());
        }
        golem.getCarried().set(slot, got);
        golem.carriedChanged();
        c.setChanged();
        if (!got.isEmpty()) golem.playSound(SoundEvents.BUNDLE_INSERT, 0.6F, 1.2F);
        return !got.isEmpty();
    }

    protected static boolean sourceHas(ServerLevel level, @Nullable BlockPos from, Predicate<ItemStack> want) {
        if (from == null) return false;
        Container c = Storage.containerAt(level, from);
        return c != null && has(c, want);
    }
}
