package com.workshopcraft.simplegolems.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplegolems.entity.Job;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

// a marker stick used on a golem: the next block clicks are that golem's setup steps
public record MarkerSetup(UUID golem, Job job) {
    public static final Codec<MarkerSetup> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("golem").forGetter(MarkerSetup::golem),
            Job.CODEC.fieldOf("job").forGetter(MarkerSetup::job)).apply(i, MarkerSetup::new));
    public static final StreamCodec<ByteBuf, MarkerSetup> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, MarkerSetup::golem, Job.STREAM_CODEC, MarkerSetup::job, MarkerSetup::new);
}
