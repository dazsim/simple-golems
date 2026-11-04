package com.workshopcraft.simplegolems.neoforge;

//? if neoforge {
/*import com.workshopcraft.simplegolems.entity.work.ExternalStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
//? if >=1.21.9 {
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
//?} else {
/^import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
^///?}
import org.jspecify.annotations.Nullable;

// an item handler capability as golem storage
//? if >=1.21.9 {
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
//?} else {
/^final class NeoForgeStore extends ExternalStore {
    private final IItemHandler handler;

    private NeoForgeStore(IItemHandler handler) {
        this.handler = handler;
    }

    static @Nullable NeoForgeStore at(ServerLevel level, BlockPos pos) {
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        return h == null || h.getSlots() == 0 ? null : new NeoForgeStore(h);
    }

    @Override
    public int getContainerSize() {
        return handler.getSlots();
    }

    @Override
    public ItemStack getItem(int slot) {
        return handler.getStackInSlot(slot);
    }

    @Override
    protected ItemStack take(int slot, int count) {
        ItemStack in = handler.getStackInSlot(slot);
        if (in.isEmpty()) return ItemStack.EMPTY;
        return handler.extractItem(slot, Math.min(count, in.getMaxStackSize()), false);
    }

    @Override
    public int put(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;
        ItemStack left = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), simulate);
        return stack.getCount() - left.getCount();
    }
}
^///?}
*///?}
