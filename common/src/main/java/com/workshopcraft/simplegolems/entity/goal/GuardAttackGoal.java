package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

// walks up to the target and swings at the weapon's own attack speed; hits a creeper and runs before it can go off
public class GuardAttackGoal extends Goal {
    private static final int[] FLEE_ANGLES = {0, 25, -25, 50, -50, 80, -80, 120, -120};
    private static final double SAFE = 64.0, FLEE_SPEED = 2.0, CREEPER_REACH = 2.75 * 2.75;
    private final GolemEntity golem;
    private int cooldown, repath, stuck;
    private boolean fleeing;
    private double best;

    public GuardAttackGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = golem.getTarget();
        return golem.canGuard() && t != null && t.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repath = stuck = 0;
        fleeing = false;
        best = Double.MAX_VALUE;
        golem.setAggressive(true);
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
        golem.setAggressive(false);
    }

    private int interval() {
        return Mth.clamp(Mth.ceil(20.0 / Math.max(0.1, golem.getAttributeValue(Attributes.ATTACK_SPEED))), 4, 60);
    }

    private void strike(LivingEntity t) {
        golem.swing(InteractionHand.MAIN_HAND);
        golem.doHurtTarget((ServerLevel) golem.level(), t);
        cooldown = interval();
    }

    // straight away from the creeper if it can, turning aside when a wall is in the way
    private void flee(Creeper c) {
        Vec3 away = golem.position().subtract(c.position()).multiply(1.0, 0.0, 1.0);
        away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, golem.getYRot() + 180.0F) : away.normalize();
        for (int a : FLEE_ANGLES) {
            Vec3 d = away.yRot(a * Mth.DEG_TO_RAD);
            Path path = golem.getNavigation().createPath(BlockPos.containing(golem.getX() + d.x * 10.0, golem.getY(), golem.getZ() + d.z * 10.0), 1);
            Node end = path == null ? null : path.getEndNode();
            if (end != null && c.distanceToSqr(end.x + 0.5, end.y, end.z + 0.5) > SAFE) {
                golem.getNavigation().moveTo(path, FLEE_SPEED);
                return;
            }
        }
        Vec3 pos = DefaultRandomPos.getPosAway(golem, 12, 4, c.position());
        if (pos != null) golem.getNavigation().moveTo(pos.x, pos.y, pos.z, FLEE_SPEED);
    }

    // a player's reach, so it can strike about as the fuse would light
    private boolean inCreeperReach(Creeper c) {
        return golem.isWithinMeleeAttackRange(c) || golem.distanceToSqr(c) < CREEPER_REACH && Math.abs(golem.getY() - c.getY()) < 1.5;
    }

    // like a player: strike the moment it is in reach, run until the fuse goes out, and only come back once it has
    private void creeperTick(Creeper c) {
        double dist = golem.distanceToSqr(c);
        if (inCreeperReach(c) && cooldown <= 0 && golem.getSensing().hasLineOfSight(c)) {
            strike(c);
            if (c.isAlive()) {
                Vec3 away = golem.position().subtract(c.position()).multiply(1.0, 0.0, 1.0);
                if (away.lengthSqr() > 1.0E-4) golem.push(away.normalize().scale(0.35).add(0.0, 0.1, 0.0));
                flee(c);
                fleeing = true;
                repath = 10;
            }
            return;
        }
        boolean lit = c.getSwellDir() > 0 || c.getSwelling(1.0F) > 0.0F;
        if (!lit && dist > SAFE) fleeing = false;
        if (lit && !fleeing && cooldown <= 0 && c.getSwelling(1.0F) < 0.3F && dist < 16.0) {
            golem.getNavigation().moveTo(c, 1.6);
            return;
        }
        if (fleeing || lit) {
            if (!fleeing) repath = 0;
            fleeing = true;
            if (dist < SAFE + 9.0) {
                if (golem.getNavigation().isDone() || --repath <= 0) {
                    repath = 10;
                    flee(c);
                }
            } else {
                golem.getNavigation().stop();
            }
            return;
        }
        if (--repath <= 0) {
            repath = 5;
            golem.getNavigation().moveTo(c, 1.3);
        }
    }

    @Override
    public void tick() {
        LivingEntity t = golem.getTarget();
        if (t == null) return;
        golem.usePower(1);
        golem.markFighting();
        golem.getLookControl().setLookAt(t, 30.0F, 30.0F);
        if (cooldown > 0) cooldown--;
        if (t instanceof Creeper c && "hit_and_back_off".equals(Config.guardCreepers)) {
            creeperTick(c);
            return;
        }
        double dist = golem.distanceToSqr(t);
        boolean reach = golem.isWithinMeleeAttackRange(t);
        if (--repath <= 0) {
            repath = reach ? 10 : 6;
            golem.getNavigation().moveTo(t, 1.2);
        }
        if (reach && cooldown <= 0 && golem.getSensing().hasLineOfSight(t)) {
            strike(t);
            stuck = 0;
            return;
        }
        if (reach || dist < best - 0.5) {
            best = Math.min(best, dist);
            stuck = 0;
        } else if (++stuck > (recentAttacker(t) ? 200 : 80)) {
            golem.skipTarget(t);
            golem.setTarget(null);
        }
    }

    private boolean recentAttacker(LivingEntity t) {
        return golem.getLastHurtByMob() == t && golem.tickCount - golem.getLastHurtByMobTimestamp() < 200;
    }
}
