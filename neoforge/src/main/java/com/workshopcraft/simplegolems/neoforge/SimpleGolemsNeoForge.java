package com.workshopcraft.simplegolems.neoforge;

import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.work.Storage;
import com.workshopcraft.simplegolems.item.MarkerStickItem;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(SimpleGolems.MOD_ID)
public class SimpleGolemsNeoForge {
    public SimpleGolemsNeoForge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC, "simplegolems.toml");
        modBus.addListener(NeoForgeConfig::onLoad);
        Storage.external = NeoForgeStore::at;
        if (ModList.get().isLoaded("exdeorum")) ExDeorumCompat.init();
        modBus.addListener(SimpleGolemsNeoForge::onRegister);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(ModContent.GOLEM_ENTITY, GolemEntity.createAttributes().build()));
        NeoForge.EVENT_BUS.addListener(SimpleGolemsNeoForge::onUseBlock);
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        InteractionResult r = MarkerStickItem.onUseBlock(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec());
        if (r != InteractionResult.PASS) {
            e.setCanceled(true);
            e.setCancellationResult(r);
        }
    }

    private static void onRegister(RegisterEvent e) {
        e.register(Registries.DATA_COMPONENT_TYPE, h -> ModContent.registerComponents(h::register));
        e.register(Registries.ENTITY_TYPE, h -> ModContent.registerEntities(h::register));
        e.register(Registries.ITEM, h -> ModContent.registerItems(h::register));
        e.register(Registries.CREATIVE_MODE_TAB, h -> ModContent.registerCreativeTabs(h::register));
    }
}
