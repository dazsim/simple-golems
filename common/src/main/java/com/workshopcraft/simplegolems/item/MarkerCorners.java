package com.workshopcraft.simplegolems.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import io.netty.buffer.ByteBuf;

public record MarkerCorners(BlockPos first, Optional<BlockPos> second) {
    public static final Codec<MarkerCorners> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("first").forGetter(MarkerCorners::first),
            BlockPos.CODEC.optionalFieldOf("second").forGetter(MarkerCorners::second)).apply(i, MarkerCorners::new));
    public static final StreamCodec<ByteBuf, MarkerCorners> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MarkerCorners::first, ByteBufCodecs.optional(BlockPos.STREAM_CODEC), MarkerCorners::second, MarkerCorners::new);
}
