package com.workshopcraft.simplegolems.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

// golems keep off treetops: no standing on leaves, and lumber golems never stand on a trunk
public class GolemNavigation extends GroundPathNavigation {
    private final GolemEntity golem;

    public GolemNavigation(GolemEntity golem, Level level) {
        super(golem, level);
        this.golem = golem;
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        nodeEvaluator = new WalkNodeEvaluator() {
            @Override
            public PathType getPathType(PathfindingContext context, int x, int y, int z) {
                PathType type = super.getPathType(context, x, y, z);
                if (type != PathType.WALKABLE) return type;
                BlockPos below = new BlockPos(x, y - 1, z);
                return offLimits(context.getBlockState(below), context.getBlockState(below.below())) ? PathType.BLOCKED : type;
            }
        };
        return new PathFinder(nodeEvaluator, maxVisitedNodes);
    }

    private boolean offLimits(BlockState floor, BlockState under) {
        if (floor.getBlock() instanceof LeavesBlock) return true;
        return golem.getJob() == Job.LUMBER && floor.is(BlockTags.LOGS) && (under.is(BlockTags.LOGS) || under.is(BlockTags.SUBSTRATE_OVERWORLD));
    }
}
