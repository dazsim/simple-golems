package com.workshopcraft.simplegolems.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;

public record WorkArea(BlockPos min, BlockPos max) {
    public static final int DEFAULT_RADIUS = 8;
    public static final int MAX_SIZE = 32;

    public static final Codec<WorkArea> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("min").forGetter(WorkArea::min),
            BlockPos.CODEC.fieldOf("max").forGetter(WorkArea::max)).apply(i, WorkArea::of));
    public static final StreamCodec<io.netty.buffer.ByteBuf, WorkArea> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WorkArea::min, BlockPos.STREAM_CODEC, WorkArea::max, WorkArea::of);

    public static WorkArea of(BlockPos a, BlockPos b) {
        return new WorkArea(BlockPos.min(a, b), BlockPos.max(a, b));
    }

    public static WorkArea around(BlockPos home) {
        return of(home.offset(-DEFAULT_RADIUS, -4, -DEFAULT_RADIUS), home.offset(DEFAULT_RADIUS, 8, DEFAULT_RADIUS));
    }

    public static boolean fits(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) < MAX_SIZE && Math.abs(a.getY() - b.getY()) < MAX_SIZE && Math.abs(a.getZ() - b.getZ()) < MAX_SIZE;
    }

    public boolean contains(BlockPos p) {
        return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY()
                && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }

    public boolean containsColumn(BlockPos p) {
        return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }

    public AABB box() {
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    public BlockPos centre() {
        return new BlockPos((min.getX() + max.getX()) / 2, (min.getY() + max.getY()) / 2, (min.getZ() + max.getZ()) / 2);
    }

    public int sizeX() { return max.getX() - min.getX() + 1; }
    public int sizeY() { return max.getY() - min.getY() + 1; }
    public int sizeZ() { return max.getZ() - min.getZ() + 1; }
}
