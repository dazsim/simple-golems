package com.workshopcraft.simplegolems.fabric;

import com.workshopcraft.simplegolems.client.CookieHud;
import com.workshopcraft.simplegolems.client.GolemMesh;
import com.workshopcraft.simplegolems.client.GolemRenderer;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class SimpleGolemsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(GolemRenderer.LAYER, GolemMesh::create);
        EntityRendererRegistry.register(ModContent.GOLEM_ENTITY, GolemRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(CookieHud::tick);
    }
}
