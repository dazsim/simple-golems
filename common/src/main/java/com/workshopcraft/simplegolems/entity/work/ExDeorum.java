package com.workshopcraft.simplegolems.entity.work;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

// set by the NeoForge side when Ex Deorum is loaded; the sifter and hammer programs only work then
public final class ExDeorum {
    public interface Hooks {
        boolean isSieve(BlockEntity be);

        boolean hasMesh(BlockEntity be);

        // something is in the sieve already
        boolean busy(BlockEntity be);

        boolean canSift(BlockEntity be, ItemStack stack);

        void fill(BlockEntity be, ItemStack one);

        // one turn of the handle; results pop out above the sieve
        void sift(BlockEntity be, ServerLevel level);

        int siftTicks();

        boolean canHammer(ItemStack tool, ItemStack block);
    }

    public static @Nullable Hooks hooks;

    private ExDeorum() {}

    public static boolean loaded() {
        return hooks != null;
    }
}
