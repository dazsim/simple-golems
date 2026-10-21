package com.workshopcraft.simplegolems.neoforge;

import com.workshopcraft.simplegolems.entity.work.ExDeorum;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thedarkcolour.exdeorum.blockentity.AbstractSieveBlockEntity;
import thedarkcolour.exdeorum.blockentity.CompressedSieveBlockEntity;
import thedarkcolour.exdeorum.blockentity.SieveBlockEntity;
import thedarkcolour.exdeorum.config.EConfig;
import thedarkcolour.exdeorum.recipe.RecipeUtil;
import thedarkcolour.exdeorum.tag.EItemTags;

// only loaded when Ex Deorum is installed; a golem works a sieve like a player, one sieve at a time
final class ExDeorumCompat implements ExDeorum.Hooks {
    // the compressed sieve's step, which Ex Deorum keeps private
    private static final float COMPRESSED_STEP = 0.075F;

    static void init() {
        ExDeorum.hooks = new ExDeorumCompat();
    }

    @Override
    public boolean isSieve(BlockEntity be) {
        return be instanceof AbstractSieveBlockEntity && (be instanceof SieveBlockEntity || be instanceof CompressedSieveBlockEntity);
    }

    @Override
    public boolean hasMesh(BlockEntity be) {
        return be instanceof AbstractSieveBlockEntity s && !s.getLogic().getMesh().isEmpty();
    }

    @Override
    public boolean busy(BlockEntity be) {
        return be instanceof AbstractSieveBlockEntity s && !s.getLogic().getContents().isEmpty();
    }

    @Override
    public boolean canSift(BlockEntity be, ItemStack stack) {
        return !stack.isEmpty() && be instanceof AbstractSieveBlockEntity s && !s.getLogic().getMesh().isEmpty() && s.getLogic().isValidInput(stack);
    }

    @Override
    public void fill(BlockEntity be, ItemStack one) {
        if (be instanceof AbstractSieveBlockEntity s) s.getLogic().startSifting(one);
    }

    @Override
    public void sift(BlockEntity be, ServerLevel level) {
        if (be instanceof AbstractSieveBlockEntity s) {
            s.getLogic().sift(be instanceof CompressedSieveBlockEntity ? COMPRESSED_STEP : SieveBlockEntity.SIEVE_INTERVAL, level.getGameTime());
        }
    }

    @Override
    public int siftTicks() {
        return EConfig.SERVER.sieveIntervalTicks.get();
    }

    @Override
    public boolean canHammer(ItemStack tool, ItemStack block) {
        if (tool.is(EItemTags.COMPRESSED_HAMMERS)) return RecipeUtil.getCompressedHammerRecipe(block.getItem()) != null;
        return tool.is(EItemTags.HAMMERS) && RecipeUtil.getHammerRecipe(block.getItem()) != null;
    }
}
