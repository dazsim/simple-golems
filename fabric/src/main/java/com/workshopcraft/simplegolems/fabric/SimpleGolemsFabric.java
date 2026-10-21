package com.workshopcraft.simplegolems.fabric;

import com.workshopcraft.simplegolems.Config;
import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.entity.work.Storage;
import com.workshopcraft.simplegolems.item.MarkerStickItem;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class SimpleGolemsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.loadFile(FabricLoader.getInstance().getConfigDir().resolve("simplegolems.toml"));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> Config.loadFile(FabricLoader.getInstance().getConfigDir().resolve("simplegolems.toml")));
        ModContent.registerAllDirect();
        Storage.external = FabricStore::at;
        FabricDefaultAttributeRegistry.register(ModContent.GOLEM_ENTITY, GolemEntity.createAttributes());
        UseBlockCallback.EVENT.register(MarkerStickItem::onUseBlock);
    }
}
