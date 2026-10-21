package com.workshopcraft.simplegolems.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import io.netty.buffer.ByteBuf;

// the first two clicks are the corners of an area; programs that work between blocks use every click, up to four
public record MarkerCorners(BlockPos first, Optional<BlockPos> second, List<BlockPos> more) {
    public static final int MAX_POINTS = 4;
    public static final Codec<MarkerCorners> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("first").forGetter(MarkerCorners::first),
            BlockPos.CODEC.optionalFieldOf("second").forGetter(MarkerCorners::second),
            BlockPos.CODEC.listOf().optionalFieldOf("more", List.of()).forGetter(MarkerCorners::more)).apply(i, MarkerCorners::new));
    public static final StreamCodec<ByteBuf, MarkerCorners> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MarkerCorners::first, ByteBufCodecs.optional(BlockPos.STREAM_CODEC), MarkerCorners::second,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_POINTS)), MarkerCorners::more, MarkerCorners::new);

    public MarkerCorners(BlockPos first, Optional<BlockPos> second) {
        this(first, second, List.of());
    }

    public static MarkerCorners of(List<BlockPos> p) {
        return new MarkerCorners(p.get(0), p.size() > 1 ? Optional.of(p.get(1)) : Optional.empty(), p.size() > 2 ? List.copyOf(p.subList(2, p.size())) : List.of());
    }

    public List<BlockPos> points() {
        List<BlockPos> out = new ArrayList<>();
        out.add(first);
        second.ifPresent(out::add);
        out.addAll(more);
        return out;
    }
}
