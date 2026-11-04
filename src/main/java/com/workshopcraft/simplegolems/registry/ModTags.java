package com.workshopcraft.simplegolems.registry;

import com.workshopcraft.simplegolems.SimpleGolems;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

// tags a data pack can change; see the README
public final class ModTags {
    public static final TagKey<Block> LOGS = block("lumber_logs");
    public static final TagKey<Item> LOG_ITEMS = item("lumber_logs");
    public static final TagKey<Item> SAPLINGS = item("lumber_saplings");
    public static final TagKey<Block> CROPS = block("farmer_crops");
    public static final TagKey<Block> MINER_NEVER_DIG = block("miner_never_dig");
    public static final TagKey<Item> HAMMERS = item("hammers");
    public static final TagKey<Item> LIGHTS = item("lamplighter_lights");
    public static final TagKey<EntityType<?>> GUARD_IGNORES = TagKey.create(Registries.ENTITY_TYPE, SimpleGolems.id("guard_ignores"));

    private ModTags() {}

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, SimpleGolems.id(name));
    }

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, SimpleGolems.id(name));
    }
}
