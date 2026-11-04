package com.workshopcraft.simplegolems.item;

//? if <1.20.5 {
/*import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;

// a shaped recipe whose result carries the "nbt" object from its json, used for the job golem recipes
public class ShapedNbtSerializer extends ShapedRecipe.Serializer {
    @Override
    public ShapedRecipe fromJson(Identifier id, JsonObject json) {
        ShapedRecipe r = super.fromJson(id, json);
        JsonObject result = GsonHelper.getAsJsonObject(json, "result");
        if (!result.has("nbt")) return r;
        ItemStack out = r.getResultItem(RegistryAccess.EMPTY).copy();
        if (JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, result.get("nbt")) instanceof CompoundTag tag) out.getOrCreateTag().merge(tag);
        return new ShapedRecipe(id, r.getGroup(), r.category(), r.getWidth(), r.getHeight(), r.getIngredients(), out, r.showNotification());
    }
}
*///?}
