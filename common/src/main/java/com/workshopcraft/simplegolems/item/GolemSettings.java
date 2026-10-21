package com.workshopcraft.simplegolems.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import io.netty.buffer.ByteBuf;

public record GolemSettings(int fuel, Optional<WorkArea> area, List<BlockPos> points) {
    public static final GolemSettings EMPTY = new GolemSettings(0, Optional.empty(), List.of());
    public static final Codec<GolemSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("fuel", 0).forGetter(GolemSettings::fuel),
            WorkArea.CODEC.optionalFieldOf("area").forGetter(GolemSettings::area),
            BlockPos.CODEC.listOf().optionalFieldOf("points", List.of()).forGetter(GolemSettings::points)).apply(i, GolemSettings::new));
    public static final StreamCodec<ByteBuf, GolemSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GolemSettings::fuel, ByteBufCodecs.optional(WorkArea.STREAM_CODEC), GolemSettings::area,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MarkerCorners.MAX_POINTS)), GolemSettings::points, GolemSettings::new);
}
