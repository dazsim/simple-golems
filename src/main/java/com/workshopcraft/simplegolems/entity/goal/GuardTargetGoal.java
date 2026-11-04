package com.workshopcraft.simplegolems.entity.goal;

import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.registry.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

// picks what a guard fights: hostile mobs in its area (or near its owner), or whatever hit it first
public class GuardTargetGoal extends Goal {
    private final GolemEntity golem;
    private @Nullable LivingEntity found;
    private int scan;

    public GuardTargetGoal(GolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    // never players, villagers, traders, pets, animals, golems or anything in the guard_ignores tag
    public static boolean mayAttack(GolemEntity golem, LivingEntity e, boolean hitFirst) {
        if (e == golem || !e.isAlive() || !(e instanceof Mob) || !e.canBeSeenAsEnemy()) return false;
        if (e instanceof AbstractGolem || e instanceof AbstractVillager) return false;
        if (e instanceof Animal && !(e instanceof Enemy)) return false;
        if (e instanceof OwnableEntity o && o.getOwnerReference() != null) return false;
        if (e.is(ModTags.GUARD_IGNORES)) return false;
        if (e instanceof Creeper && "ignore".equals(Config.guardCreepers)) return false;
        if (hitFirst) return true;
        if (!(e instanceof Enemy)) return false;
        return !(e instanceof NeutralMob) && !(e instanceof AbstractPiglin && !(e instanceof PiglinBrute));
    }

    private boolean inRange(LivingEntity e, double extra) {
        return golem.guardBox(extra).intersects(e.getBoundingBox());
    }

    private @Nullable LivingEntity attacker() {
        LivingEntity hit = golem.getLastHurtByMob();
        if (hit == null || golem.tickCount - golem.getLastHurtByMobTimestamp() > 100) return null;
        return mayAttack(golem, hit, true) && !golem.skipped(hit) && inRange(hit, Config.guardChaseRange) ? hit : null;
    }

    @Override
    public boolean canUse() {
        if (!golem.canGuard()) return false;
        found = attacker();
        if (found != null) return true;
        if (--scan > 0) return false;
        scan = 10;
        List<Mob> near = golem.level().getEntitiesOfClass(Mob.class, golem.guardBox(1.0),
                e -> mayAttack(golem, e, false) && !golem.skipped(e));
        near.sort(Comparator.comparingDouble(golem::distanceToSqr));
        for (int i = 0; i < Math.min(3, near.size()); i++) {
            Mob e = near.get(i);
            Path p = golem.getNavigation().createPath(e, 1);
            // no path while it is in the air (just summoned or knocked back), so look again soon
            if (p == null && !golem.onGround() && !golem.isInLiquid()) return false;
            if (golem.isWithinMeleeAttackRange(e) || p != null && p.canReach()) {
                found = e;
                return true;
            }
            golem.skipTarget(e);
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = golem.getTarget();
        return t != null && golem.canGuard() && mayAttack(golem, t, true) && !golem.skipped(t) && inRange(t, Config.guardChaseRange);
    }

    @Override
    public void start() {
        golem.setTarget(found);
    }

    @Override
    public void stop() {
        golem.setTarget(null);
        found = null;
    }
}
