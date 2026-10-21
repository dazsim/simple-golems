package com.workshopcraft.simplegolems.registry;

import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.work.ExDeorum;
import com.workshopcraft.simplegolems.item.CardItem;
import com.workshopcraft.simplegolems.item.GolemItem;
import com.workshopcraft.simplegolems.item.GolemSettings;
import com.workshopcraft.simplegolems.item.MarkerCorners;
import com.workshopcraft.simplegolems.item.MarkerSetup;
import com.workshopcraft.simplegolems.item.MarkerStickItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ModContent {
    public static final DataComponentType<Job> JOB = DataComponentType.<Job>builder().persistent(Job.CODEC).networkSynchronized(Job.STREAM_CODEC).build();
    public static final DataComponentType<DyeColor> COLOUR = DataComponentType.<DyeColor>builder().persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC).build();
    public static final DataComponentType<GolemSettings> SETTINGS = DataComponentType.<GolemSettings>builder().persistent(GolemSettings.CODEC).networkSynchronized(GolemSettings.STREAM_CODEC).build();
    public static final DataComponentType<MarkerCorners> MARKER = DataComponentType.<MarkerCorners>builder().persistent(MarkerCorners.CODEC).networkSynchronized(MarkerCorners.STREAM_CODEC).build();
    public static final DataComponentType<MarkerSetup> MARKER_SETUP = DataComponentType.<MarkerSetup>builder().persistent(MarkerSetup.CODEC).networkSynchronized(MarkerSetup.STREAM_CODEC).build();

    public static final ResourceKey<EntityType<?>> GOLEM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleGolems.id("golem"));
    public static EntityType<GolemEntity> GOLEM_ENTITY;

    public static Item GOLEM_CORE;
    public static Item GOLEM;
    public static Item CLAY_COOKIE;
    public static Item MARKER_STICK;
    public static final Map<Job, CardItem> CARDS = new EnumMap<>(Job.class);
    public static CreativeModeTab TAB;

    private ModContent() {}

    public static void registerComponents(BiConsumer<Identifier, DataComponentType<?>> r) {
        r.accept(SimpleGolems.id("job"), JOB);
        r.accept(SimpleGolems.id("colour"), COLOUR);
        r.accept(SimpleGolems.id("settings"), SETTINGS);
        r.accept(SimpleGolems.id("marker"), MARKER);
        r.accept(SimpleGolems.id("marker_setup"), MARKER_SETUP);
    }

    public static void registerEntities(BiConsumer<Identifier, EntityType<?>> r) {
        GOLEM_ENTITY = EntityType.Builder.of(GolemEntity::new, MobCategory.MISC)
                .sized(0.7F, 0.9F).eyeHeight(0.65F).clientTrackingRange(10).build(GOLEM_KEY);
        r.accept(GOLEM_KEY.identifier(), GOLEM_ENTITY);
    }

    private static Item.Properties item(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SimpleGolems.id(name)));
    }

    public static void registerItems(BiConsumer<Identifier, Item> r) {
        GOLEM_CORE = new Item(item("golem_core"));
        r.accept(SimpleGolems.id("golem_core"), GOLEM_CORE);
        GOLEM = new GolemItem(item("golem").stacksTo(1));
        r.accept(SimpleGolems.id("golem"), GOLEM);
        CLAY_COOKIE = new Item(item("clay_cookie"));
        r.accept(SimpleGolems.id("clay_cookie"), CLAY_COOKIE);
        MARKER_STICK = new MarkerStickItem(item("marker_stick").stacksTo(1));
        r.accept(SimpleGolems.id("marker_stick"), MARKER_STICK);
        for (Job job : Job.values()) {
            if (job == Job.BLANK) continue;
            String name = job.getSerializedName() + "_card";
            CardItem card = new CardItem(job, item(name).stacksTo(16));
            CARDS.put(job, card);
            r.accept(SimpleGolems.id(name), card);
        }
    }

    public static ItemStack card(Job job) {
        CardItem card = CARDS.get(job);
        return card == null ? ItemStack.EMPTY : new ItemStack(card);
    }

    public static void registerCreativeTabs(BiConsumer<Identifier, CreativeModeTab> r) {
        TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup." + SimpleGolems.MOD_ID))
                .icon(() -> GolemItem.of(Job.LUMBER, DyeColor.ORANGE))
                .displayItems((params, out) -> {
                    out.accept(GOLEM_CORE);
                    out.accept(CLAY_COOKIE);
                    out.accept(MARKER_STICK);
                    for (Job job : Job.values()) if (shown(job) && CARDS.containsKey(job)) out.accept(CARDS.get(job));
                    for (Job job : Job.values()) if (shown(job)) out.accept(GolemItem.of(job, null));
                    for (DyeColor c : DyeColor.values()) out.accept(GolemItem.of(Job.BLANK, c));
                })
                .build();
        r.accept(SimpleGolems.id("main"), TAB);
    }

    // the Ex Deorum programs only show when it is installed
    private static boolean shown(Job job) {
        return !job.needsExDeorum() || ExDeorum.loaded();
    }

    public static <T> BiConsumer<Identifier, T> direct(Registry<? super T> registry) {
        return (id, value) -> Registry.register(registry, id, value);
    }

    public static void registerAllDirect() {
        registerComponents(direct(BuiltInRegistries.DATA_COMPONENT_TYPE));
        registerEntities(direct(BuiltInRegistries.ENTITY_TYPE));
        registerItems(direct(BuiltInRegistries.ITEM));
        registerCreativeTabs(direct(BuiltInRegistries.CREATIVE_MODE_TAB));
    }
}
