package com.workshopcraft.simplegolems.entity.work;

import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// feeds pairs of grown animals so each kind in its area breeds up to the configured count; feed comes from storage
public class RancherWork implements JobWork {
    public static final RancherWork INSTANCE = new RancherWork();

    @Override
    public boolean wants(ItemEntity item) {
        return false;
    }

    @Override
    public boolean keeps(GolemEntity golem, int slot) {
        return true;
    }

    @Override
    public String supplies() {
        return "feed";
    }

    private static boolean ready(Animal a) {
        return a.isAlive() && !a.isBaby() && a.getAge() == 0 && a.canFallInLove();
    }

    // grown animals of each kind that may still breed, when the kind is under the count and has a partner
    private static List<Animal> breedable(ServerLevel level, WorkArea area) {
        Map<EntityType<?>, List<Animal>> kinds = new HashMap<>();
        for (Animal a : level.getEntitiesOfClass(Animal.class, area.box(), Animal::isAlive)) kinds.computeIfAbsent(a.getType(), k -> new ArrayList<>()).add(a);
        List<Animal> out = new ArrayList<>();
        for (List<Animal> all : kinds.values()) {
            long inLove = all.stream().filter(Animal::isInLove).count();
            List<Animal> ready = all.stream().filter(RancherWork::ready).toList();
            // babies to come once one more is fed; an odd one in love is waiting for its partner
            long births = (inLove + 2) / 2;
            if (all.size() + births > Config.rancherMaxAnimals || ready.size() < (inLove % 2 == 1 ? 1 : 2)) continue;
            out.addAll(ready);
        }
        return out;
    }

    private static int food(GolemEntity golem, Animal a) {
        for (int i = 0; i < GolemEntity.CARRY_SLOTS; i++) if (a.isFood(golem.getCarried().get(i))) return i;
        return -1;
    }

    @Override
    public @Nullable BlockPos findTarget(GolemEntity golem, ServerLevel level, WorkArea area, Predicate<BlockPos> skip) {
        Animal a = breedable(level, area).stream().filter(e -> food(golem, e) >= 0 && !skip.test(e.blockPosition()))
                .min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        if (a != null) golem.setSupplies(null);
        return a == null ? null : a.blockPosition();
    }

    @Override
    public @Nullable Predicate<ItemStack> restock(GolemEntity golem) {
        if (!(golem.level() instanceof ServerLevel level)) return null;
        List<Animal> want = breedable(level, area(golem)).stream().filter(e -> food(golem, e) < 0).toList();
        if (want.isEmpty()) {
            golem.setSupplies(null);
            return null;
        }
        return s -> want.stream().anyMatch(a -> a.isFood(s));
    }

    @Override
    public boolean isValid(ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isValid(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(2.5), a -> ready(a) && food(golem, a) >= 0).size() > 0;
    }

    @Override
    public int workTicks(GolemEntity golem, ServerLevel level, BlockPos pos) {
        return 10;
    }

    @Override
    public void finish(GolemEntity golem, ServerLevel level, BlockPos pos) {
        List<Animal> ok = breedable(level, area(golem));
        Animal a = level.getEntitiesOfClass(Animal.class, golem.getBoundingBox().inflate(3.0), e -> ok.contains(e) && food(golem, e) >= 0).stream()
                .min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        if (a == null) return;
        int slot = food(golem, a);
        golem.getCarried().get(slot).shrink(1);
        golem.carriedChanged();
        a.setInLove(null);
        golem.playSound(SoundEvents.GENERIC_EAT.value(), 0.5F, 1.2F);
    }
}
