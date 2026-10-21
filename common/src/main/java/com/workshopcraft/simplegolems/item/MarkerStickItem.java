package com.workshopcraft.simplegolems.item;

import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.entity.WorkArea;
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

    public MarkerStickItem(Properties properties) {
        super(properties);
    }

    public static @Nullable WorkArea area(ItemStack stack) {
        MarkerCorners m = stack.get(ModContent.MARKER);
        return m == null || m.second().isEmpty() ? null : WorkArea.of(m.first(), m.second().get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (player != null && player.isSecondaryUseActive()) return clear(player, stack);
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        MarkerCorners m = stack.get(ModContent.MARKER);
        if (m == null || m.points().size() >= MarkerCorners.MAX_POINTS) {
            stack.set(ModContent.MARKER, new MarkerCorners(pos.immutable(), Optional.empty()));
            tell(player, "marker.first", pos.getX(), pos.getY(), pos.getZ());
            context.getLevel().playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
        } else if (!WorkArea.fits(m.first(), pos)) {
            tell(player, "marker.too_big", Config.maxAreaWidth);
        } else if (m.second().isEmpty()) {
            stack.set(ModContent.MARKER, new MarkerCorners(m.first(), Optional.of(pos.immutable())));
            WorkArea a = WorkArea.of(m.first(), pos);
            tell(player, "marker.second", a.sizeX(), a.sizeY(), a.sizeZ());
            context.getLevel().playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        } else {
            List<BlockPos> more = new ArrayList<>(m.more());
            more.add(pos.immutable());
            stack.set(ModContent.MARKER, new MarkerCorners(m.first(), m.second(), more));
            tell(player, "marker.point", more.size() + 2, pos.getX(), pos.getY(), pos.getZ());
            context.getLevel().playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, 1.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return clear(player, player.getItemInHand(hand));
        return InteractionResult.PASS;
    }

    private InteractionResult clear(Player player, ItemStack stack) {
        if (!stack.has(ModContent.MARKER)) return InteractionResult.PASS;
        if (!player.level().isClientSide()) {
            stack.remove(ModContent.MARKER);
            tell(player, "marker.cleared");
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
        if (m == null) {
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
