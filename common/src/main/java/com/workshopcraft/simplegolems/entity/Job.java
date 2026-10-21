package com.workshopcraft.simplegolems.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import com.workshopcraft.simplegolems.registry.ModTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public enum Job implements StringRepresentable {
    BLANK("blank", 0xF0F0F0),
    LUMBER("lumber", 0x58AA40),
    FARMER("farmer", 0xECC03A),
    MINER("miner", 0xCE463A),
    COURIER("courier", 0x3E78D6),
    SWEEPER("sweeper", 0xA65CC4),
    COBBLEGEN("cobblegen", 0x8A8F96),
    SIFTER("sifter", 0xC9B27A),
    HAMMER("hammer", 0x7A5236),
    SHEPHERD("shepherd", 0xE8E4DA),
    RANCHER("rancher", 0xB86E3C),
    COMPOSTER("composter", 0x6E9A3A),
    SMELTER("smelter", 0xE07A2A),
    BEEKEEPER("beekeeper", 0xF2B93B),
    FISHER("fisher", 0x3FA7B5),
    LAMPLIGHTER("lamplighter", 0xF5D76E);

    public static final Codec<Job> CODEC = StringRepresentable.fromEnum(Job::values);
    public static final StreamCodec<ByteBuf, Job> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Job::ordinal);

    private final String name;
    public final int colour;

    Job(String name, int colour) {
        this.name = name;
        this.colour = colour;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static Job byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : BLANK;
    }

    public boolean usesTool(ItemStack stack) {
        return switch (this) {
            case LUMBER -> stack.is(ItemTags.AXES);
            case FARMER -> stack.is(ItemTags.HOES);
            case MINER -> stack.is(ItemTags.PICKAXES);
            case SWEEPER -> stack.is(Items.BRUSH) || stack.is(ItemTags.SHOVELS);
            case COBBLEGEN -> stack.is(ItemTags.PICKAXES);
            case HAMMER -> stack.is(ModTags.HAMMERS);
            case SHEPHERD, BEEKEEPER -> stack.is(Items.SHEARS);
            case FISHER -> stack.is(Items.FISHING_ROD);
            default -> false;
        };
    }

    public boolean needsArea() {
        return this == MINER || this == COURIER;
    }

    public boolean needsTool() {
        return this == LUMBER || this == FARMER || this == MINER || this == COBBLEGEN || this == HAMMER || this == SHEPHERD || this == FISHER;
    }

    // blocks clicked with the marker stick, in order: the job works between them instead of in an area
    public int points() {
        return switch (this) {
            case COBBLEGEN -> 2;
            case SIFTER, HAMMER, COMPOSTER -> 3;
            case SMELTER -> 4;
            default -> 0;
        };
    }

    // what each marker click is during setup, in order: points() blocks, or the two corners of an area
    public List<String> steps() {
        return switch (this) {
            case BLANK -> List.of();
            case COBBLEGEN -> List.of("mine", "drop_chest");
            case SIFTER -> List.of("input_chest", "sieve", "output_chest");
            case HAMMER -> List.of("input_chest", "spot", "output_chest");
            case COMPOSTER -> List.of("input_chest", "composter", "output_chest");
            case SMELTER -> List.of("input_chest", "fuel_chest", "furnace", "output_chest");
            case COURIER -> List.of("from_corner", "to_corner");
            default -> List.of("corner", "opposite_corner");
        };
    }

    // skyblock programs that need Ex Deorum
    public boolean needsExDeorum() {
        return this == SIFTER || this == HAMMER;
    }
}
