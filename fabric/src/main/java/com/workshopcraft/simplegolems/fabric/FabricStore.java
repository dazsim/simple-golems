package com.workshopcraft.simplegolems.fabric;

import com.workshopcraft.simplegolems.entity.work.ExternalStore;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// a Transfer API item storage as golem storage
final class FabricStore extends ExternalStore {
    private final Storage<ItemVariant> storage;
    private final List<StorageView<ItemVariant>> views = new ArrayList<>();

    private FabricStore(Storage<ItemVariant> storage) {
        this.storage = storage;
        for (StorageView<ItemVariant> v : storage) views.add(v);
    }

    static @Nullable FabricStore at(ServerLevel level, BlockPos pos) {
        Storage<ItemVariant> s = ItemStorage.SIDED.find(level, pos, null);
        if (s == null || !s.supportsInsertion() && !s.supportsExtraction()) return null;
        FabricStore f = new FabricStore(s);
        return f.views.isEmpty() ? null : f;
    }

    @Override
    public int getContainerSize() {
        return views.size();
    }

    @Override
    public ItemStack getItem(int slot) {
        StorageView<ItemVariant> v = views.get(slot);
        return v.isResourceBlank() || v.getAmount() <= 0 ? ItemStack.EMPTY : v.getResource().toStack((int) Math.min(Integer.MAX_VALUE, v.getAmount()));
    }

    @Override
    protected ItemStack take(int slot, int count) {
        StorageView<ItemVariant> v = views.get(slot);
        if (v.isResourceBlank()) return ItemStack.EMPTY;
        ItemVariant r = v.getResource();
        try (Transaction tx = Transaction.openOuter()) {
            long got = v.extract(r, Math.min(count, r.toStack().getMaxStackSize()), tx);
            tx.commit();
            return got <= 0 ? ItemStack.EMPTY : r.toStack((int) got);
        }
    }

    @Override
    public int put(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long in = storage.insert(ItemVariant.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return (int) in;
        }
    }
}
