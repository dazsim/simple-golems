package com.workshopcraft.simplegolems.neoforge;

import com.workshopcraft.simplegolems.entity.work.ExternalStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

// an item handler capability as golem storage
final class NeoForgeStore extends ExternalStore {
    private final ResourceHandler<ItemResource> handler;

    private NeoForgeStore(ResourceHandler<ItemResource> handler) {
        this.handler = handler;
    }

    static @Nullable NeoForgeStore at(ServerLevel level, BlockPos pos) {
        ResourceHandler<ItemResource> h = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        return h == null || h.size() == 0 ? null : new NeoForgeStore(h);
    }

    @Override
    public int getContainerSize() {
        return handler.size();
    }

    @Override
    public ItemStack getItem(int slot) {
        ItemResource r = handler.getResource(slot);
        return r.isEmpty() ? ItemStack.EMPTY : r.toStack(handler.getAmountAsInt(slot));
    }

    @Override
    protected ItemStack take(int slot, int count) {
        ItemResource r = handler.getResource(slot);
        if (r.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int got = handler.extract(slot, r, Math.min(count, r.toStack().getMaxStackSize()), tx);
            tx.commit();
            return got <= 0 ? ItemStack.EMPTY : r.toStack(got);
        }
    }

    @Override
    public int put(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int in = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return in;
        }
    }
}
