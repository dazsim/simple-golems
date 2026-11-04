package com.workshopcraft.simplegolems.forge;

//? if forge {
/*import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.work.Storage;
import com.workshopcraft.simplegolems.item.MarkerStickItem;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

@Mod(SimpleGolems.MOD_ID)
public class SimpleGolemsForge {
    public SimpleGolemsForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        Config.loadFile(FMLPaths.CONFIGDIR.get().resolve("simplegolems.toml"));
        Storage.external = ForgeStore::at;
        if (ModList.get().isLoaded("exdeorum")) ExDeorumCompat.init();
        modBus.addListener(SimpleGolemsForge::onRegister);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(ModContent.GOLEM_ENTITY, GolemEntity.createAttributes().build()));
        MinecraftForge.EVENT_BUS.addListener(SimpleGolemsForge::onUseBlock);
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent e) -> Config.loadFile(FMLPaths.CONFIGDIR.get().resolve("simplegolems.toml")));
        if (FMLEnvironment.dist.isClient()) SimpleGolemsForgeClient.init(modBus);
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        InteractionResult r = MarkerStickItem.onUseBlock(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec());
        if (r != InteractionResult.PASS) {
            e.setCanceled(true);
            e.setCancellationResult(r);
        }
    }

    private static void onRegister(RegisterEvent e) {
        e.register(Registries.ENTITY_TYPE, h -> ModContent.registerEntities(h::register));
        e.register(Registries.ITEM, h -> ModContent.registerItems(h::register));
        e.register(Registries.CREATIVE_MODE_TAB, h -> ModContent.registerCreativeTabs(h::register));
        e.register(Registries.RECIPE_SERIALIZER, h -> ModContent.registerRecipeSerializers(h::register));
    }
}
*///?}
