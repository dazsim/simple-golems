package com.workshopcraft.simplegolems.entity.work;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// another mod's item storage seen as a Container; items only go in through put, never through setItem
public abstract class ExternalStore implements Container {
    // how many of the stack would go in (or went in)
    public abstract int put(ItemStack stack, boolean simulate);

    protected abstract ItemStack take(int slot, int count);

    @Override
    public ItemStack removeItem(int slot, int count) {
        return count <= 0 ? ItemStack.EMPTY : take(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return take(slot, Integer.MAX_VALUE);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) take(slot, Integer.MAX_VALUE);
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }
}
