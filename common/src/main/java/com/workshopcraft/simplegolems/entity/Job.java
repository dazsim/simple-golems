package com.workshopcraft.simplegolems.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum Job implements StringRepresentable {
    BLANK("blank", 0xF0F0F0),
    LUMBER("lumber", 0x58AA40),
    FARMER("farmer", 0xECC03A),
    MINER("miner", 0xCE463A),
    COURIER("courier", 0x3E78D6),
    SWEEPER("sweeper", 0xA65CC4);

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
            case SWEEPER -> stack.is(Items.BRUSH);
            default -> false;
        };
    }

    public boolean needsTool() {
        return this == LUMBER || this == FARMER || this == MINER;
    }
}
