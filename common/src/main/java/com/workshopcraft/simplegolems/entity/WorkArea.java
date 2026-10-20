package com.workshopcraft.simplegolems.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;

// first and second are kept in click order; the courier takes from the first corner and delivers to the second
public record WorkArea(BlockPos first, BlockPos second) {
    public static final int DEFAULT_RADIUS = 8;
    public static final int MAX_SIZE = 32;

    public static final Codec<WorkArea> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("first").forGetter(WorkArea::first),
            BlockPos.CODEC.fieldOf("second").forGetter(WorkArea::second)).apply(i, WorkArea::new));
    public static final StreamCodec<io.netty.buffer.ByteBuf, WorkArea> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WorkArea::first, BlockPos.STREAM_CODEC, WorkArea::second, WorkArea::new);

    public WorkArea {
        first = first.immutable();
        second = second.immutable();
    }

    public static WorkArea of(BlockPos a, BlockPos b) {
        return new WorkArea(a, b);
    }

    public BlockPos min() {
        return BlockPos.min(first, second);
    }

    public BlockPos max() {
        return BlockPos.max(first, second);
    }

    public static WorkArea around(BlockPos home) {
        return of(home.offset(-DEFAULT_RADIUS, -4, -DEFAULT_RADIUS), home.offset(DEFAULT_RADIUS, 8, DEFAULT_RADIUS));
    }

    public WorkArea down(int layers) {
        return new WorkArea(new BlockPos(first.getX(), max().getY(), first.getZ()), new BlockPos(second.getX(), min().getY() - layers + 1, second.getZ()));
    }

    public AABB box(double grow) {
        return box().inflate(grow, 0, grow);
    }

    public static boolean fits(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) < MAX_SIZE && Math.abs(a.getY() - b.getY()) < MAX_SIZE && Math.abs(a.getZ() - b.getZ()) < MAX_SIZE;
    }

    public boolean contains(BlockPos p) {
        BlockPos min = min(), max = max();
        return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY()
                && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }

    public boolean near(BlockPos p, int grow) {
        BlockPos min = min(), max = max();
        return p.getX() >= min.getX() - grow && p.getX() <= max.getX() + grow && p.getY() >= min.getY() - grow && p.getY() <= max.getY() + grow
                && p.getZ() >= min.getZ() - grow && p.getZ() <= max.getZ() + grow;
    }

    public boolean containsColumn(BlockPos p) {
        BlockPos min = min(), max = max();
        return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }

    public AABB box() {
        BlockPos min = min(), max = max();
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    public BlockPos centre() {
        BlockPos min = min(), max = max();
        return new BlockPos((min.getX() + max.getX()) / 2, (min.getY() + max.getY()) / 2, (min.getZ() + max.getZ()) / 2);
    }

    public int sizeX() { return Math.abs(second.getX() - first.getX()) + 1; }
    public int sizeY() { return Math.abs(second.getY() - first.getY()) + 1; }
    public int sizeZ() { return Math.abs(second.getZ() - first.getZ()) + 1; }
}
