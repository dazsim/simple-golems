package com.workshopcraft.simplegolems.entity;

import com.workshopcraft.simplegolems.registry.ModTags;
import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.item.GolemSettings;
import com.workshopcraft.simplegolems.entity.goal.EscapeHazardGoal;
import com.workshopcraft.simplegolems.entity.goal.FetchToolGoal;
import com.workshopcraft.simplegolems.entity.goal.FollowOwnerGoal;
import com.workshopcraft.simplegolems.entity.goal.GetOffTreeGoal;
import com.workshopcraft.simplegolems.entity.work.JobWork;
import com.workshopcraft.simplegolems.entity.work.MinerWork;
import com.workshopcraft.simplegolems.entity.work.Storage;
import com.workshopcraft.simplegolems.entity.goal.StrollInAreaGoal;
import com.workshopcraft.simplegolems.entity.goal.WaitGoal;
import com.workshopcraft.simplegolems.entity.work.CourierWork;
import com.workshopcraft.simplegolems.entity.work.DigOutGoal;
import com.workshopcraft.simplegolems.entity.work.WorkGoal;
import com.workshopcraft.simplegolems.item.CardItem;
import com.workshopcraft.simplegolems.item.MarkerStickItem;
import com.workshopcraft.simplegolems.item.MarkerCorners;
import com.workshopcraft.simplegolems.entity.work.ExDeorum;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.tags.ItemTags;

public class GolemEntity extends AbstractGolem {
    public static final int COOKIE_TICKS = 6000;

    private static final EntityDataAccessor<Integer> DATA_JOB = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOUR = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_CARRIED = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COOKIES = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TROUBLE = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);

    private ItemStack cookies = ItemStack.EMPTY;
    public static final int CARRY_SLOTS = 4;
    private final NonNullList<ItemStack> carried = NonNullList.withSize(CARRY_SLOTS, ItemStack.EMPTY);
    private int fuel;
    private @Nullable WorkArea customArea;
    private List<BlockPos> points = List.of();
    private @Nullable String supplies;
    private @Nullable BlockPos home;
    private @Nullable UUID owner;
    private boolean storageTrouble;
    private boolean saplingTrouble;
    private boolean blocksTrouble;
    private boolean needBlocks;
    private Trouble lastTrouble = Trouble.NONE;
    private int thanks;
    public float bubble, bubbleO;
    public Trouble shownTrouble = Trouble.NONE;

    public GolemEntity(EntityType<? extends GolemEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GolemNavigation(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MINING_EFFICIENCY);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new EscapeHazardGoal(this));
        goalSelector.addGoal(0, new FetchToolGoal(this));
        goalSelector.addGoal(2, new GetOffTreeGoal(this));
        goalSelector.addGoal(2, new DigOutGoal(this));
        goalSelector.addGoal(1, new WaitGoal(this));
        goalSelector.addGoal(3, new FollowOwnerGoal(this));
        goalSelector.addGoal(4, new WorkGoal(this));
        goalSelector.addGoal(6, new StrollInAreaGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_JOB, 0);
        b.define(DATA_COLOUR, -1);
        b.define(DATA_POWERED, false);
        b.define(DATA_WAITING, false);
        b.define(DATA_TROUBLE, 0);
        b.define(DATA_COOKIES, 0);
        b.define(DATA_CARRIED, ItemStack.EMPTY);
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
    public int getShownCookies() { return entityData.get(DATA_COOKIES); }
    public void setCookies(ItemStack s) { cookies = s; updatePowered(); }
    // the four carry slots; call carriedChanged after changing a stack in place
    public NonNullList<ItemStack> getCarried() { return carried; }

    public void carriedChanged() {
        for (int i = 0; i < CARRY_SLOTS; i++) if (carried.get(i).isEmpty()) carried.set(i, ItemStack.EMPTY);
        ItemStack shown = carried.stream().filter(c -> !c.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        entityData.set(DATA_CARRIED, shown.copyWithCount(1));
    }

    public void setCarried(int slot, ItemStack s) {
        carried.set(slot, s);
        carriedChanged();
    }

    public void clearCarried() {
        Collections.fill(carried, ItemStack.EMPTY);
        carriedChanged();
    }

    public boolean carryEmpty() {
        return carried.stream().allMatch(ItemStack::isEmpty);
    }

    // no empty slot left
    public boolean carryFull() {
        return carried.stream().noneMatch(ItemStack::isEmpty);
    }

    public boolean canCarry(ItemStack stack) {
        for (ItemStack c : carried) {
            if (c.isEmpty() || ItemStack.isSameItemSameComponents(c, stack) && c.getCount() < c.getMaxStackSize()) return true;
        }
        return false;
    }

    public boolean carries(Predicate<ItemStack> test) {
        return carried.stream().anyMatch(c -> !c.isEmpty() && test.test(c));
    }

    // tops up matching stacks first, then empty slots; what does not fit is left in the stack
    public void addCarried(ItemStack stack) {
        for (ItemStack c : carried) {
            if (stack.isEmpty()) break;
            if (!c.isEmpty() && ItemStack.isSameItemSameComponents(c, stack)) {
                int n = Math.min(stack.getCount(), c.getMaxStackSize() - c.getCount());
                c.grow(n);
                stack.shrink(n);
            }
        }
        for (int i = 0; i < CARRY_SLOTS && !stack.isEmpty(); i++) {
            if (carried.get(i).isEmpty()) carried.set(i, stack.split(stack.getMaxStackSize()));
        }
        carriedChanged();
    }

    public ItemStack getShownCarried() {
        return entityData.get(DATA_CARRIED);
    }
    public int getFuel() { return fuel; }
    public @Nullable WorkArea getCustomArea() { return customArea; }
    public void setCustomArea(@Nullable WorkArea area) { customArea = area; }
    public List<BlockPos> getPoints() { return points; }
    public void setPoints(List<BlockPos> p) { points = List.copyOf(p); }
    public @Nullable UUID getOwner() { return owner; }
    public void setOwner(@Nullable UUID uuid) { owner = uuid; }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
    }

    public BlockPos getHome() {
        return home == null ? blockPosition() : home;
    }

    public WorkArea getArea() {
        if (getJob().points() > 0 && points.size() >= 2) return pointsBox();
        if (customArea == null) return WorkArea.around(getHome());
        // miners and couriers may work a tall area; other jobs keep to 32 blocks high
        return getJob() == Job.MINER || getJob() == Job.COURIER ? customArea : customArea.capHeight();
    }

    // the blocks a program works between, with room to stand around them
    private WorkArea pointsBox() {
        BlockPos min = points.get(0), max = points.get(0);
        for (BlockPos p : points) {
            min = BlockPos.min(min, p);
            max = BlockPos.max(max, p);
        }
        return WorkArea.of(min.offset(-1, -1, -1), max.offset(1, 2, 1));
    }

    // same rules as a player: tool tier speed, Efficiency through the mining_efficiency attribute, Haste
    public float digSpeed(BlockState state) {
        float speed = getTool().getDestroySpeed(state);
        if (speed > 1.0F) speed += (float) getAttributeValue(Attributes.MINING_EFFICIENCY);
        if (MobEffectUtil.hasDigSpeed(this)) speed *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
        return speed;
    }

    public int digTicks(BlockState state, BlockPos pos) {
        float hardness = state.getDestroySpeed(level(), pos);
        if (hardness <= 0) return 2;
        return Mth.clamp(Mth.ceil(hardness * 30.0F / Math.max(0.1F, digSpeed(state))), 2, 100) + 2;
    }

    public boolean canHarvest(BlockState state) {
        return !state.requiresCorrectToolForDrops() || getTool().isCorrectToolForDrops(state);
    }

    public boolean hasTool() {
        return !getJob().needsTool() || !getTool().isEmpty();
    }

    public boolean isWaiting() {
        if (level().isClientSide()) return entityData.get(DATA_WAITING);
        return getJob() != Job.BLANK && (!isPowered() || !hasTool() || !canWorkHere());
    }

    public boolean canWorkHere() {
        Job job = getJob();
        if (job.needsArea() && customArea == null || !Config.jobEnabled(job) || points.size() < job.points()) return false;
        if (job.needsExDeorum() && !ExDeorum.loaded()) return false;
        return job != Job.MINER || !(level() instanceof ServerLevel sl) || sl.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    public boolean hasStorageTrouble() {
        return storageTrouble;
    }

    public void setStorageTrouble(boolean b) {
        storageTrouble = b;
    }

    public void setBlocksTrouble(boolean b) {
        blocksTrouble = b;
    }

    // the miner has steps to build or water to seal and nothing to do it with
    public boolean needsBlocks() {
        return needBlocks;
    }

    public void setNeedsBlocks(boolean b) {
        needBlocks = b;
        if (!b) blocksTrouble = false;
    }

    // what a program is waiting for (lang key part), or null
    public void setSupplies(@Nullable String what) {
        supplies = what;
    }

    public @Nullable String getSupplies() {
        return supplies;
    }

    public void setSaplingTrouble(boolean b) {
        saplingTrouble = b;
    }

    public Trouble getTrouble() {
        if (level().isClientSide()) return Trouble.byId(entityData.get(DATA_TROUBLE));
        Job job = getJob();
        if (job == Job.BLANK) return Trouble.NONE;
        if (!isPowered()) return Trouble.FOOD;
        if (!hasTool()) return Trouble.TOOL;
        if (storageTrouble) return Trouble.STORAGE;
        if (saplingTrouble && job == Job.LUMBER) return Trouble.SAPLING;
        if (blocksTrouble && job == Job.MINER) return Trouble.BLOCKS;
        if (supplies != null) return Trouble.SUPPLIES;
        return Trouble.NONE;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            Trouble t = getTrouble();
            bubbleO = bubble;
            if (t != Trouble.NONE) shownTrouble = t;
            bubble = Mth.clamp(bubble + (t != Trouble.NONE ? 0.1F : -0.1F), 0.0F, 1.0F);
            return;
        }
        if (getY() < level().getMinY() && Config.voidRescue) rescueFromVoid();
        if (tickCount % 40 == 0 && level() instanceof ServerLevel sl) recheckTrouble(sl);
        if (thanks > 0) thanks--;
        if (tickCount % 10 == 0) {
            entityData.set(DATA_WAITING, isWaiting());
            Trouble t = getTrouble();
            if (t != Trouble.NONE) {
                thanks = 0;
            } else if (lastTrouble != Trouble.NONE && getJob() != Job.BLANK && level() instanceof ServerLevel sl) {
                thanks = 40;
                sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 1, 0.25, 0.1, 0.25, 0.0);
            }
            lastTrouble = t;
            entityData.set(DATA_TROUBLE, (t == Trouble.NONE && thanks > 0 ? Trouble.THANKS : t).ordinal());
        }
    }

    // storage may have been emptied or added, and a sapling may have turned up
    private void recheckTrouble(ServerLevel sl) {
        JobWork work = JobWork.of(getJob());
        if (storageTrouble && (work == null || work.load(this).stream()
                .allMatch(c -> work.dropOff(this, sl, work.area(this), c, p -> false) != null))) storageTrouble = false;
        if (blocksTrouble && (carries(MinerWork::isBuildingBlock) || Storage.nearest(sl, work == null ? getArea() : work.area(this), position(),
                c -> c.hasAnyMatching(st -> st.is(MinerWork.BUILDING)), p -> false) != null)) blocksTrouble = false;
        if (saplingTrouble && Storage.nearest(sl, getArea(), position(), c -> c.hasAnyMatching(st -> st.is(ModTags.SAPLINGS)), p -> false) != null) {
            saplingTrouble = false;
        }
    }

    @Override
    protected void onBelowWorld() {
        if (Config.voidRescue) rescueFromVoid();
        else super.onBelowWorld();
    }

    // fell out of the world: back up on the highest safe spot nearby, or home if there is none
    private void rescueFromVoid() {
        BlockPos at = safeSpotNear(getBlockX(), getBlockZ());
        if (at == null) at = safeSpotNear(getHome().getX(), getHome().getZ());
        if (at == null) at = getHome().getY() > level().getMinY() ? getHome() : level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, getHome());
        getNavigation().stop();
        teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
    }

    private @Nullable BlockPos safeSpotNear(int x, int z) {
        Level level = level();
        for (int r = 0; r <= 32; r++) {
            BlockPos best = null;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r || !level.hasChunkAt(new BlockPos(x + dx, 0, z + dz))) continue;
                    BlockPos p = highestStand(level, x + dx, z + dz);
                    if (p != null && (best == null || p.getY() > best.getY())) best = p;
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static @Nullable BlockPos highestStand(Level level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int y = top; y > level.getMinY(); y--) {
            BlockPos p = new BlockPos(x, y, z), below = p.below();
            if (!clear(level, p) || !clear(level, p.above())) continue;
            BlockState floor = level.getBlockState(below);
            if (floor.isFaceSturdy(level, below, Direction.UP) && floor.getFluidState().isEmpty() && !floor.is(BlockTags.FIRE) && !floor.is(Blocks.MAGMA_BLOCK)) return p;
        }
        return null;
    }

    private static boolean clear(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty() && s.getFluidState().isEmpty() && !s.is(BlockTags.FIRE);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    protected void updatePowered() {
        if (level().isClientSide()) return;
        entityData.set(DATA_POWERED, fuel > 0 || !cookies.isEmpty());
        entityData.set(DATA_COOKIES, cookies.getCount());
    }

    // one cookie is eaten when the last one runs out, so a golem fed n cookies works for n * COOKIE_TICKS
    public boolean usePower(int ticks) {
        if (fuel <= 0) {
            if (cookies.isEmpty()) {
                updatePowered();
                return false;
            }
            cookies.shrink(1);
            fuel += Config.cookieTicks;
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
        for (int i = 0; i < CARRY_SLOTS; i++) carried.set(i, items.size() > 2 + i ? items.get(2 + i) : ItemStack.EMPTY);
        carriedChanged();
        GolemSettings s = stack.getOrDefault(ModContent.SETTINGS, GolemSettings.EMPTY);
        fuel = s.fuel();
        customArea = s.area().orElse(null);
        points = List.copyOf(s.points());
        updatePowered();
    }

    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ModContent.GOLEM);
        stack.set(ModContent.JOB, getJob());
        if (getColour() != null) stack.set(ModContent.COLOUR, getColour());
        List<ItemStack> items = new ArrayList<>(List.of(getTool().copy(), cookies.copy()));
        for (ItemStack c : carried) items.add(c.copy());
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        stack.set(ModContent.SETTINGS, new GolemSettings(fuel, Optional.ofNullable(customArea), points));
        if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());
        return stack;
    }

    public boolean canBeChangedBy(Player player) {
        return owner == null || owner.equals(player.getUUID()) || player.hasInfiniteMaterials();
    }

    private void tell(Player player, String key, Object... args) {
        player.sendOverlayMessage(Component.translatable("message.simplegolems." + key, args));
    }

    private void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean server = !level().isClientSide();
        Job job = getJob();
        if (hand != InteractionHand.MAIN_HAND && held.isEmpty()) return InteractionResult.PASS;

        if (held.isEmpty() && player.isSecondaryUseActive()) {
            if (!canBeChangedBy(player)) {
                if (server) tell(player, "not_yours");
                return InteractionResult.SUCCESS;
            }
            if (server) {
                give(player, toItem());
                playSound(SoundEvents.DECORATED_POT_INSERT, 0.8F, 1.4F);
                discard();
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(Items.STICK) && player.isSecondaryUseActive()) {
            if (job == Job.BLANK) return InteractionResult.PASS;
            if (server) {
                if (!canBeChangedBy(player)) {
                    tell(player, "not_yours");
                } else {
                    give(player, ModContent.card(job));
                    setJob(Job.BLANK);
                    playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.2F);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (held.getItem() instanceof CardItem card) {
            if (server) {
                if (job != Job.BLANK) {
                    tell(player, "has_card");
                } else if (!canBeChangedBy(player)) {
                    tell(player, "not_yours");
                } else {
                    setJob(card.job);
                    held.consume(1, player);
                    if (!getTool().isEmpty() && !card.job.usesTool(getTool())) {
                        give(player, getTool());
                        setTool(ItemStack.EMPTY);
                    }
                    playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.5F);
                    tell(player, "programmed", Component.translatable("job.simplegolems." + card.job.getSerializedName()));
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (held.getItem() instanceof MarkerStickItem) {
            if (server) {
                WorkArea marked = MarkerStickItem.area(held);
                if (!canBeChangedBy(player)) {
                    tell(player, "not_yours");
                } else if (player.isSecondaryUseActive()) {
                    customArea = null;
                    points = List.of();
                    setHome(blockPosition());
                    tell(player, "marker.reset");
                } else if (job.points() > 0) {
                    MarkerCorners m = held.get(ModContent.MARKER);
                    List<BlockPos> clicked = m == null ? List.of() : m.points();
                    if (clicked.size() < job.points()) {
                        tell(player, "marker.points_needed." + job.getSerializedName());
                    } else {
                        points = List.copyOf(clicked.subList(0, job.points()));
                        customArea = null;
                        tell(player, "marker.points_given", job.points());
                        playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.5F);
                    }
                } else if (marked == null) {
                    tell(player, "marker.unfinished");
                } else {
                    customArea = marked;
                    tell(player, "marker.given", marked.sizeX(), marked.sizeY(), marked.sizeZ());
                    playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.5F);
                }
                if (player instanceof ServerPlayer sp) {
                    if (job.points() > 0) for (BlockPos p : points) MarkerStickItem.outline((ServerLevel) level(), sp, WorkArea.of(p, p), 0x60E060);
                    else MarkerStickItem.outline((ServerLevel) level(), sp, getArea(), 0x60E060);
                }
            }
            return InteractionResult.SUCCESS;
        }

        DyeColor dye = held.get(DataComponents.DYE);
        if (dye != null && held.getItem() instanceof DyeItem) {
            if (dye == getColour()) return InteractionResult.PASS;
            if (server) {
                setColour(dye);
                held.consume(1, player);
                level().playSound(null, this, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModContent.CLAY_COOKIE)) {
            int room = held.getMaxStackSize() - cookies.getCount();
            if (room <= 0) {
                if (server) tell(player, "cookies_full");
                return InteractionResult.SUCCESS;
            }
            if (server) {
                int n = Math.min(room, held.getCount());
                if (cookies.isEmpty()) cookies = held.copyWithCount(n);
                else cookies.grow(n);
                if (!player.hasInfiniteMaterials()) held.shrink(n);
                updatePowered();
                playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.6F);
                tell(player, "fed", cookies.getCount());
            }
            return InteractionResult.SUCCESS;
        }

        if (isTool(held)) {
            if (server) {
                if (!job.usesTool(held)) {
                    tell(player, "wrong_tool." + job.getSerializedName());
                } else {
                    ItemStack old = getTool();
                    setTool(held.split(1));
                    give(player, old);
                    playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 0.8F, 1.3F);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty()) {
            if (server) {
                if (!carryEmpty()) {
                    for (ItemStack c : carried) if (!c.isEmpty()) give(player, c);
                    clearCarried();
                } else {
                    tellStatus(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // a miner only steps down one block at a time, so it does not drop into a cave it cannot climb out of
    @Override
    public int getMaxFallDistance() {
        return getJob() == Job.MINER ? 1 : super.getMaxFallDistance();
    }

    public static boolean isTool(ItemStack stack) {
        for (Job j : Job.values()) if (j.usesTool(stack)) return true;
        return stack.is(ItemTags.SHOVELS);
    }

    public void tellStatus(Player player) {
        Job job = getJob();
        Component name = Component.translatable("job.simplegolems." + job.getSerializedName());
        if (job == Job.BLANK) {
            tell(player, "status.blank");
        } else if (job.needsTool() && getTool().isEmpty()) {
            tell(player, "status.no_tool", name);
        } else if (fuel <= 0 && cookies.isEmpty()) {
            tell(player, "status.no_power", name);
        } else if (job.needsArea() && customArea == null) {
            tell(player, "status.no_area", name);
        } else if (points.size() < job.points()) {
            tell(player, "marker.points_needed." + job.getSerializedName());
        } else if (job.needsExDeorum() && !ExDeorum.loaded()) {
            tell(player, "status.no_exdeorum", name);
        } else if (!Config.jobEnabled(job)) {
            tell(player, "status.disabled", name);
        } else if (supplies != null) {
            tell(player, "status.needs." + supplies, name);
        } else if (!canWorkHere()) {
            tell(player, "status.no_griefing", name);
        } else if (job == Job.COURIER && level() instanceof ServerLevel sl && !CourierWork.hasRoute(sl, customArea)) {
            tell(player, "status.no_route", name);
        } else {
            WorkArea a = getArea();
            int minutes = (fuel + cookies.getCount() * Config.cookieTicks) / 1200;
            tell(player, "status.working", name, minutes, a.sizeX(), a.sizeY(), a.sizeZ());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.store("job", Job.CODEC, getJob());
        if (getColour() != null) out.store("colour", DyeColor.CODEC, getColour());
        out.store("cookies", ItemStack.OPTIONAL_CODEC, cookies);
        if (thanks > 0) out.putInt("thanks", thanks);
        if (getTrouble() != Trouble.NONE) out.putString("trouble", getTrouble().name().toLowerCase(Locale.ROOT));
        out.store("carry", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(carried));
        out.putInt("fuel", fuel);
        if (customArea != null) out.store("area", WorkArea.CODEC, customArea);
        if (!points.isEmpty()) out.store("points", BlockPos.CODEC.listOf(), points);
        if (home != null) out.store("home", BlockPos.CODEC, home);
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setJob(in.read("job", Job.CODEC).orElse(Job.BLANK));
        setColour(in.read("colour", DyeColor.CODEC).orElse(null));
        cookies = in.read("cookies", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        // alpha.6 and older kept one stack under "carried": it becomes slot 0
        List<ItemStack> carry = in.read("carry", ItemStack.OPTIONAL_CODEC.listOf())
                .orElseGet(() -> List.of(in.read("carried", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY)));
        for (int i = 0; i < CARRY_SLOTS; i++) carried.set(i, i < carry.size() ? carry.get(i) : ItemStack.EMPTY);
        carriedChanged();
        fuel = in.getIntOr("fuel", 0);
        customArea = in.read("area", WorkArea.CODEC).orElse(null);
        points = List.copyOf(in.read("points", BlockPos.CODEC.listOf()).orElse(List.of()));
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
        for (ItemStack s : List.of(getTool(), cookies)) if (!s.isEmpty()) spawnAtLocation(level, s.copy());
        for (ItemStack s : carried) if (!s.isEmpty()) spawnAtLocation(level, s.copy());
        setTool(ItemStack.EMPTY);
        cookies = ItemStack.EMPTY;
        clearCarried();
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
