package com.workshopcraft.simplegolems.forge;

//? if forge {
/*import com.workshopcraft.simplegolems.entity.work.ExternalStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jspecify.annotations.Nullable;

// an item handler capability as golem storage
final class ForgeStore extends ExternalStore {
    private final IItemHandler handler;

    private ForgeStore(IItemHandler handler) {
        this.handler = handler;
    }

    static @Nullable ForgeStore at(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        return h == null || h.getSlots() == 0 ? null : new ForgeStore(h);
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
*///?}
