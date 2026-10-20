package com.workshopcraft.simplegolems.entity;

import com.workshopcraft.simplegolems.item.GolemSettings;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GolemEntity extends AbstractGolem {
    public static final int COOKIE_TICKS = 6000;

    private static final EntityDataAccessor<Integer> DATA_JOB = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOUR = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BOOLEAN);

    private ItemStack cookies = ItemStack.EMPTY;
    private ItemStack carried = ItemStack.EMPTY;
    private int fuel;
    private @Nullable WorkArea customArea;
    private @Nullable BlockPos home;
    private @Nullable UUID owner;

    public GolemEntity(EntityType<? extends GolemEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_JOB, 0);
        b.define(DATA_COLOUR, -1);
        b.define(DATA_POWERED, false);
    }

    public Job getJob() {
        return Job.byId(entityData.get(DATA_JOB));
    }

    public void setJob(Job job) {
        entityData.set(DATA_JOB, job.ordinal());
    }

    public @Nullable DyeColor getColour() {
        int c = entityData.get(DATA_COLOUR);
        return c < 0 ? null : DyeColor.byId(c);
    }

    public void setColour(@Nullable DyeColor colour) {
        entityData.set(DATA_COLOUR, colour == null ? -1 : colour.getId());
    }

    public boolean isPowered() {
        return entityData.get(DATA_POWERED);
    }

    public ItemStack getTool() {
        return getMainHandItem();
    }

    public void setTool(ItemStack stack) {
        setItemSlot(EquipmentSlot.MAINHAND, stack);
    }

    public ItemStack getCookies() { return cookies; }
    public void setCookies(ItemStack s) { cookies = s; updatePowered(); }
    public ItemStack getCarried() { return carried; }
    public void setCarried(ItemStack s) { carried = s; }
    public int getFuel() { return fuel; }
    public @Nullable WorkArea getCustomArea() { return customArea; }
    public void setCustomArea(@Nullable WorkArea area) { customArea = area; }
    public @Nullable UUID getOwner() { return owner; }
    public void setOwner(@Nullable UUID uuid) { owner = uuid; }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
    }

    public BlockPos getHome() {
        return home == null ? blockPosition() : home;
    }

    public WorkArea getArea() {
        return customArea != null ? customArea : WorkArea.around(getHome());
    }

    protected void updatePowered() {
        if (!level().isClientSide()) entityData.set(DATA_POWERED, fuel > 0 || !cookies.isEmpty());
    }

    // one cookie is eaten when the last one runs out, so a golem fed n cookies works for n * COOKIE_TICKS
    public boolean usePower(int ticks) {
        if (fuel <= 0) {
            if (cookies.isEmpty()) {
                updatePowered();
                return false;
            }
            cookies.shrink(1);
            fuel += COOKIE_TICKS;
        }
        fuel = Math.max(0, fuel - ticks);
        updatePowered();
        return true;
    }

    public void fromItem(ItemStack stack) {
        setJob(stack.getOrDefault(ModContent.JOB, Job.BLANK));
        setColour(stack.get(ModContent.COLOUR));
        List<ItemStack> items = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).allItemsCopyStream().toList();
        setTool(items.size() > 0 ? items.get(0) : ItemStack.EMPTY);
        cookies = items.size() > 1 ? items.get(1) : ItemStack.EMPTY;
        carried = items.size() > 2 ? items.get(2) : ItemStack.EMPTY;
        GolemSettings s = stack.getOrDefault(ModContent.SETTINGS, GolemSettings.EMPTY);
        fuel = s.fuel();
        customArea = s.area().orElse(null);
        updatePowered();
    }

    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ModContent.GOLEM);
        stack.set(ModContent.JOB, getJob());
        if (getColour() != null) stack.set(ModContent.COLOUR, getColour());
        List<ItemStack> items = new ArrayList<>(List.of(getTool().copy(), cookies.copy(), carried.copy()));
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        stack.set(ModContent.SETTINGS, new GolemSettings(fuel, Optional.ofNullable(customArea)));
        if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());
        return stack;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.store("job", Job.CODEC, getJob());
        if (getColour() != null) out.store("colour", DyeColor.CODEC, getColour());
        out.store("cookies", ItemStack.OPTIONAL_CODEC, cookies);
        out.store("carried", ItemStack.OPTIONAL_CODEC, carried);
        out.putInt("fuel", fuel);
        if (customArea != null) out.store("area", WorkArea.CODEC, customArea);
        if (home != null) out.store("home", BlockPos.CODEC, home);
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setJob(in.read("job", Job.CODEC).orElse(Job.BLANK));
        setColour(in.read("colour", DyeColor.CODEC).orElse(null));
        cookies = in.read("cookies", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        carried = in.read("carried", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        fuel = in.getIntOr("fuel", 0);
        customArea = in.read("area", WorkArea.CODEC).orElse(null);
        home = in.read("home", BlockPos.CODEC).orElse(null);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        updatePowered();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        spawnAtLocation(level, new ItemStack(ModContent.GOLEM_CORE));
        ItemStack card = ModContent.card(getJob());
        if (!card.isEmpty()) spawnAtLocation(level, card);
        for (ItemStack s : List.of(getTool(), cookies, carried)) if (!s.isEmpty()) spawnAtLocation(level, s.copy());
        setTool(ItemStack.EMPTY);
        cookies = ItemStack.EMPTY;
        carried = ItemStack.EMPTY;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + (random.nextFloat() - random.nextFloat()) * 0.1F;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.MUD_BRICKS_STEP, 0.5F, 1.4F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
