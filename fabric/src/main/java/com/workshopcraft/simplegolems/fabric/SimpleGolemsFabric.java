package com.workshopcraft.simplegolems.fabric;

import com.workshopcraft.simplegolems.entity.GolemEntity;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class SimpleGolemsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModContent.registerAllDirect();
        FabricDefaultAttributeRegistry.register(ModContent.GOLEM_ENTITY, GolemEntity.createAttributes());
    }
}
