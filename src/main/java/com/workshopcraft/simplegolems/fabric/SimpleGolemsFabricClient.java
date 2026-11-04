package com.workshopcraft.simplegolems.fabric;

//? if fabric {
import com.workshopcraft.simplegolems.client.CookieHud;
import com.workshopcraft.simplegolems.client.GolemMesh;
import com.workshopcraft.simplegolems.client.GolemRenderer;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
//? if >=1.21.2 {
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
//?} else {
/*import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.client.GolemItemColours;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
*///?}

public class SimpleGolemsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //? if >=1.21.2 {
        ModelLayerRegistry.registerModelLayer(GolemRenderer.LAYER, GolemMesh::create);
        //?} else {
        /*EntityModelLayerRegistry.registerModelLayer(GolemRenderer.LAYER, GolemMesh::create);
        ColorProviderRegistry.ITEM.register(GolemItemColours::colour, ModContent.GOLEM);
        ItemProperties.register(ModContent.GOLEM, SimpleGolems.id("job"), (stack, level, entity, seed) -> GolemItemColours.job(stack));
        *///?}
        EntityRendererRegistry.register(ModContent.GOLEM_ENTITY, GolemRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(CookieHud::tick);
    }
}
//?}
