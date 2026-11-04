package com.workshopcraft.simplegolems.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.workshopcraft.simplegolems.SimpleGolems;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import java.util.List;
//? if >=1.20.5 {
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
//?} else {
/*import java.util.ArrayList;
import net.minecraft.nbt.ListTag;
*///?}

// item data that is a data component on 1.20.5+ and a compound under the mod id in the stack nbt before that
public final class ItemData {
    // the data version of 1.20.1, which is what golem items and saved stacks from those worlds carry
    public static final int V1_20_1 = 3465;

    private ItemData() {}

    //? if >=1.20.5 {
    public static <T> T get(ItemStack stack, DataComponentType<T> key) {
        return stack.get(key);
    }

    public static <T> T getOrDefault(ItemStack stack, DataComponentType<T> key, T fallback) {
        return stack.getOrDefault(key, fallback);
    }

    public static <T> void set(ItemStack stack, DataComponentType<T> key, T value) {
        stack.set(key, value);
    }

    public static boolean has(ItemStack stack, DataComponentType<?> key) {
        return stack.has(key);
    }

    public static void remove(ItemStack stack, DataComponentType<?> key) {
        stack.remove(key);
    }

    public static List<ItemStack> items(ItemStack stack) {
        //? if >=26.3 {
        /*return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).itemCopies().toList();
        *///?} else if >=1.21.5 {
        return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).allItemsCopyStream().toList();
        //?} else {
        /*return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList();
        *///?}
    }

    public static void setItems(ItemStack stack, List<ItemStack> items) {
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    // reads a saved stack, upgrading it first when it was saved by 1.20.1
    public static ItemStack parseStack(Tag tag, HolderLookup.Provider registries) {
        if (isLegacyStack(tag)) tag = upgradeStack(tag, V1_20_1);
        return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }
    //?} else {
    /*public record Key<T>(String name, Codec<T> codec) {}

    public static <T> T get(ItemStack stack, Key<T> key) {
        CompoundTag tag = stack.getTagElement(SimpleGolems.MOD_ID);
        if (tag == null || !tag.contains(key.name())) return null;
        return key.codec().parse(NbtOps.INSTANCE, tag.get(key.name())).result().orElse(null);
    }

    public static <T> T getOrDefault(ItemStack stack, Key<T> key, T fallback) {
        T v = get(stack, key);
        return v == null ? fallback : v;
    }

    public static <T> void set(ItemStack stack, Key<T> key, T value) {
        key.codec().encodeStart(NbtOps.INSTANCE, value).result().ifPresent(t -> stack.getOrCreateTagElement(SimpleGolems.MOD_ID).put(key.name(), t));
    }

    public static boolean has(ItemStack stack, Key<?> key) {
        CompoundTag tag = stack.getTagElement(SimpleGolems.MOD_ID);
        return tag != null && tag.contains(key.name());
    }

    public static void remove(ItemStack stack, Key<?> key) {
        CompoundTag tag = stack.getTagElement(SimpleGolems.MOD_ID);
        if (tag == null) return;
        tag.remove(key.name());
        if (tag.isEmpty()) stack.removeTagKey(SimpleGolems.MOD_ID);
    }

    public static List<ItemStack> items(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(SimpleGolems.MOD_ID);
        List<ItemStack> out = new ArrayList<>();
        if (tag != null) for (Tag t : tag.getList("items", Tag.TAG_COMPOUND)) out.add(ItemStack.of((CompoundTag) t));
        return out;
    }

    public static void setItems(ItemStack stack, List<ItemStack> items) {
        stack.getOrCreateTagElement(SimpleGolems.MOD_ID).put("items", saveStacks(items));
    }

    public static ListTag saveStacks(List<ItemStack> items) {
        ListTag list = new ListTag();
        for (ItemStack s : items) list.add(s.save(new CompoundTag()));
        return list;
    }

    *///?}

    // brings a saved stack from an older data version up to this one, a no-op on the same version
    public static Tag upgradeStack(Tag tag, int from) {
        //? if >=1.21.6 {
        int now = SharedConstants.getCurrentVersion().dataVersion().version();
        //?} else {
        /*int now = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        *///?}
        if (from >= now) return tag;
        return DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, tag), from, now).getValue();
    }

    // stacks saved by 1.20.1 have a Count byte, newer ones a count int
    public static boolean isLegacyStack(Tag tag) {
        return tag instanceof CompoundTag c && c.contains("Count") && !c.contains("count");
    }

    public static <T> void put(CompoundTag tag, String key, Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(t -> tag.put(key, t));
    }

    public static <T> java.util.Optional<T> read(CompoundTag tag, String key, Codec<T> codec) {
        Tag t = tag.get(key);
        return t == null ? java.util.Optional.empty() : codec.parse(NbtOps.INSTANCE, t).result();
    }
}
