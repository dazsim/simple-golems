package com.workshopcraft.simplegolems.item;

import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.Job;
import com.workshopcraft.simplegolems.entity.WorkArea;
import com.workshopcraft.simplegolems.entity.work.ExDeorum;
import com.workshopcraft.simplegolems.entity.work.Storage;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.sounds.SoundSource;

public class MarkerStickItem extends Item {
    private static final int FIRST_COLOUR = 0xE0C040;
    private static final int AREA_COLOUR = 0x50D0E0;
    private static final int DONE_COLOUR = 0x60E060;

    public MarkerStickItem(Properties properties) {
        super(properties);
    }

    public static @Nullable WorkArea area(ItemStack stack) {
        MarkerCorners m = stack.get(ModContent.MARKER);
        return m == null || m.second().isEmpty() ? null : WorkArea.of(m.first(), m.second().get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return click(context.getPlayer(), context.getLevel(), context.getItemInHand(), context.getClickedPos(), context.getClickedFace());
    }

    // both loaders call this before the clicked block's own use, so a chest or furnace is marked instead of opened
    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof MarkerStickItem) || player.isSpectator()) return InteractionResult.PASS;
        return click(player, level, stack, hit.getBlockPos(), hit.getDirection());
    }

    private static InteractionResult click(@Nullable Player player, Level level, ItemStack stack, BlockPos pos, Direction face) {
        MarkerSetup setup = stack.get(ModContent.MARKER_SETUP);
        if (setup == null && player != null && player.isSecondaryUseActive()) return clear(player, stack);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (setup != null) {
            setupStep(player, (ServerLevel) level, stack, setup, pos, face);
            return InteractionResult.SUCCESS;
        }
        MarkerCorners m = stack.get(ModContent.MARKER);
        if (m == null || m.points().size() >= MarkerCorners.MAX_POINTS) {
            stack.set(ModContent.MARKER, new MarkerCorners(pos.immutable(), Optional.empty()));
            tell(player, "marker.first", pos.getX(), pos.getY(), pos.getZ());
            ding(level, pos, 1.2F);
        } else if (!WorkArea.fits(m.first(), pos)) {
            tell(player, "marker.too_big", Config.maxAreaWidth);
        } else if (m.second().isEmpty()) {
            stack.set(ModContent.MARKER, new MarkerCorners(m.first(), Optional.of(pos.immutable())));
            WorkArea a = WorkArea.of(m.first(), pos);
            tell(player, "marker.second", a.sizeX(), a.sizeY(), a.sizeZ());
            ding(level, pos, 1.6F);
        } else {
            List<BlockPos> more = new ArrayList<>(m.more());
            more.add(pos.immutable());
            stack.set(ModContent.MARKER, new MarkerCorners(m.first(), m.second(), more));
            tell(player, "marker.point", more.size() + 2, pos.getX(), pos.getY(), pos.getZ());
            ding(level, pos, 1.8F);
        }
        return InteractionResult.SUCCESS;
    }

    // starts a golem's setup: the next clicks are its steps, sneaking or not
    public static void startSetup(Player player, ItemStack stack, GolemEntity golem) {
        stack.remove(ModContent.MARKER);
        stack.set(ModContent.MARKER_SETUP, new MarkerSetup(golem.getUUID(), golem.getJob()));
        tellStep(player, golem.getJob(), 0);
    }

    private static void setupStep(@Nullable Player player, ServerLevel level, ItemStack stack, MarkerSetup setup, BlockPos pos, Direction face) {
        List<String> steps = setup.job().steps();
        MarkerCorners m = stack.get(ModContent.MARKER);
        List<BlockPos> got = new ArrayList<>(m == null ? List.of() : m.points());
        if (steps.isEmpty() || got.size() >= steps.size()) {
            stack.remove(ModContent.MARKER_SETUP);
            return;
        }
        String step = steps.get(got.size());
        BlockPos p = pick(level, step, pos, face);
        if (p == null) {
            tell(player, "setup.wrong." + step);
            return;
        }
        if (!got.isEmpty() && !WorkArea.fits(got.get(0), p)) {
            tell(player, "marker.too_big", Config.maxAreaWidth);
            return;
        }
        got.add(p.immutable());
        stack.set(ModContent.MARKER, MarkerCorners.of(got));
        if (got.size() < steps.size()) {
            tellStep(player, setup.job(), got.size());
            ding(level, p, 1.2F + 0.2F * got.size());
            return;
        }
        stack.remove(ModContent.MARKER_SETUP);
        Component name = Component.translatable("job.simplegolems." + setup.job().getSerializedName());
        if (level.getEntity(setup.golem()) instanceof GolemEntity g && g.isAlive() && g.getJob() == setup.job() && (player == null || g.canBeChangedBy(player))) {
            g.applySetup(got);
            stack.remove(ModContent.MARKER);
            tell(player, "setup.done", name);
            level.playSound(null, p, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
            if (player instanceof ServerPlayer sp) for (BlockPos b : got) outline(level, sp, WorkArea.of(b, b), DONE_COLOUR);
        } else {
            tell(player, "setup.golem_gone", name);
        }
    }

    // the block a setup step takes from a click, or null when it is the wrong kind
    private static @Nullable BlockPos pick(ServerLevel level, String step, BlockPos pos, Direction face) {
        BlockState s = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return switch (step) {
            case "mine" -> be == null && !s.isAir() ? pos : null;
            case "input_chest", "fuel_chest", "output_chest", "drop_chest" -> Storage.containerAt(level, pos) != null ? pos : null;
            case "furnace" -> be instanceof AbstractFurnaceBlockEntity ? pos : null;
            case "composter" -> s.getBlock() instanceof ComposterBlock ? pos : null;
            case "sieve" -> ExDeorum.hooks == null || be != null && ExDeorum.hooks.isSieve(be) ? pos : null;
            // the hammer works on the space above the clicked block
            case "spot" -> s.canBeReplaced() ? pos : pos.relative(face);
            default -> pos;
        };
    }

    private static void tellStep(@Nullable Player player, Job job, int i) {
        List<String> steps = job.steps();
        if (player != null) player.sendOverlayMessage(Component.translatable("message.simplegolems.setup.step",
                Component.translatable("message.simplegolems.setup." + steps.get(i)), i + 1, steps.size()));
    }

    private static void ding(Level level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, pitch);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return clear(player, player.getItemInHand(hand));
        return InteractionResult.PASS;
    }

    private static InteractionResult clear(Player player, ItemStack stack) {
        if (!stack.has(ModContent.MARKER) && !stack.has(ModContent.MARKER_SETUP)) return InteractionResult.PASS;
        if (!player.level().isClientSide()) {
            boolean setup = stack.has(ModContent.MARKER_SETUP);
            stack.remove(ModContent.MARKER);
            stack.remove(ModContent.MARKER_SETUP);
            tell(player, setup ? "setup.cancelled" : "marker.cleared");
        }
        return InteractionResult.SUCCESS;
    }

    private static void tell(@Nullable Player player, String key, Object... args) {
        if (player != null) player.sendOverlayMessage(Component.translatable("message.simplegolems." + key, args));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND || !(owner instanceof ServerPlayer player) || level.getGameTime() % 10 != 0) return;
        MarkerCorners m = stack.get(ModContent.MARKER);
        if (m == null) return;
        if (stack.has(ModContent.MARKER_SETUP)) {
            for (BlockPos p : m.points()) outline(level, player, WorkArea.of(p, p), FIRST_COLOUR);
            return;
        }
        WorkArea a = m.second().map(s -> WorkArea.of(m.first(), s)).orElse(WorkArea.of(m.first(), m.first()));
        outline(level, player, a, m.second().isPresent() ? AREA_COLOUR : FIRST_COLOUR);
        for (BlockPos p : m.points()) outline(level, player, WorkArea.of(p, p), FIRST_COLOUR);
    }

    public static void outline(ServerLevel level, ServerPlayer player, WorkArea a, int colour) {
        DustParticleOptions dust = new DustParticleOptions(colour, 1.0F);
        double x0 = a.min().getX(), y0 = a.min().getY(), z0 = a.min().getZ();
        double x1 = a.max().getX() + 1, y1 = a.max().getY() + 1, z1 = a.max().getZ() + 1;
        double[][] edges = {
                {x0, y0, z0, x1, y0, z0}, {x0, y1, z0, x1, y1, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z1, x1, y1, z1},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
        for (double[] e : edges) {
            double len = Math.max(Math.abs(e[3] - e[0]), Math.max(Math.abs(e[4] - e[1]), Math.abs(e[5] - e[2])));
            int steps = (int) Math.ceil(len * 2);
            for (int i = 0; i <= steps; i++) {
                double t = steps == 0 ? 0 : (double) i / steps;
                level.sendParticles(player, dust, true, false, e[0] + (e[3] - e[0]) * t, e[1] + (e[4] - e[1]) * t, e[2] + (e[5] - e[2]) * t, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        MarkerCorners m = stack.get(ModContent.MARKER);
        MarkerSetup setup = stack.get(ModContent.MARKER_SETUP);
        if (setup != null) {
            out.accept(Component.translatable("tooltip.simplegolems.marker.setup", Component.translatable("job.simplegolems." + setup.job().getSerializedName()),
                    m == null ? 0 : m.points().size(), setup.job().steps().size()).withStyle(ChatFormatting.GRAY));
        } else if (m == null) {
            out.accept(Component.translatable("tooltip.simplegolems.marker.help").withStyle(ChatFormatting.GRAY));
        } else if (m.second().isEmpty()) {
            out.accept(Component.translatable("tooltip.simplegolems.marker.first", m.first().getX(), m.first().getY(), m.first().getZ()).withStyle(ChatFormatting.GRAY));
        } else {
            WorkArea a = WorkArea.of(m.first(), m.second().get());
            out.accept(Component.translatable("tooltip.simplegolems.marker.area", a.sizeX(), a.sizeY(), a.sizeZ()).withStyle(ChatFormatting.GRAY));
            if (!m.more().isEmpty()) out.accept(Component.translatable("tooltip.simplegolems.marker.points", m.points().size()).withStyle(ChatFormatting.GRAY));
        }
    }
}
