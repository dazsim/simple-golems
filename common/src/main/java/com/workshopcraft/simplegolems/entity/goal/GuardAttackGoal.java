package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

// walks up to the target and swings at the weapon's own attack speed; backs away from a swelling creeper
public class GuardAttackGoal extends Goal {
    private final GolemEntity golem;
    private int cooldown, repath, retreat, stuck;
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
        repath = retreat = stuck = 0;
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

    private void backOff(LivingEntity from) {
        Vec3 away = DefaultRandomPos.getPosAway(golem, 10, 4, from.position());
        if (away != null) golem.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
    }

    @Override
    public void tick() {
        LivingEntity t = golem.getTarget();
        if (t == null) return;
        golem.usePower(1);
        golem.markFighting();
        golem.getLookControl().setLookAt(t, 30.0F, 30.0F);
        if (cooldown > 0) cooldown--;
        Creeper creeper = t instanceof Creeper c && "hit_and_back_off".equals(Config.guardCreepers) ? c : null;
        double dist = golem.distanceToSqr(t);
        if (retreat > 0) {
            retreat--;
            boolean swelling = creeper != null && creeper.getSwellDir() > 0;
            if (golem.getNavigation().isDone() && dist < 64.0 && swelling) backOff(t);
            if (retreat > 0 && (swelling || retreat > 60) && dist < 100.0) return;
            retreat = 0;
            repath = 0;
        }
        boolean reach = golem.isWithinMeleeAttackRange(t);
        if (creeper != null && !reach && creeper.getSwelling(1.0F) > 0.5F) {
            retreat = 140;
            backOff(t);
            return;
        }
        if (--repath <= 0) {
            repath = reach ? 10 : 6;
            golem.getNavigation().moveTo(t, 1.2);
        }
        if (reach && cooldown <= 0 && golem.getSensing().hasLineOfSight(t)) {
            golem.swing(InteractionHand.MAIN_HAND);
            golem.doHurtTarget((ServerLevel) golem.level(), t);
            cooldown = interval();
            stuck = 0;
            if (creeper != null && t.isAlive()) {
                retreat = 140;
                backOff(t);
            }
            return;
        }
        if (reach || dist < best - 0.5) {
            best = Math.min(best, dist);
            stuck = 0;
        } else if (++stuck > 80) {
            golem.skipTarget(t);
            golem.setTarget(null);
        }
    }
}
