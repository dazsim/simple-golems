package com.workshopcraft.simplegolems.client;

//? if <1.20.5 {
/*import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.item.ItemData;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

// the golem item on 1.20.1: the body layer takes the dye tint and the job picks the accent model
public final class GolemItemColours {
    private static final int UNDYED = -4294568;
    private static final int[] DYES = {-8503, -3577293, -4558712, -7370580, -1595855, -8154043, -3578780, -11847375,
            -5601669, -9538444, -6988946, -10531212, -10271695, -10327495, -5025730, -13426663};

    private GolemItemColours() {}

    public static int colour(ItemStack stack, int layer) {
        if (layer != 0) return -1;
        DyeColor c = ItemData.get(stack, ModContent.COLOUR);
        return c == null ? UNDYED : DYES[c.getId()];
    }

    // model overrides match job ordinal / 100, since item properties are clamped to 0..1
    public static float job(ItemStack stack) {
        return ItemData.getOrDefault(stack, ModContent.JOB, Job.BLANK).ordinal() / 100.0F;
    }
}
*///?}
