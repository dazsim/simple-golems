package com.workshopcraft.simplegolems.forge;

//? if forge {
/*import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.client.CookieHud;
import com.workshopcraft.simplegolems.client.GolemItemColours;
import com.workshopcraft.simplegolems.client.GolemMesh;
import com.workshopcraft.simplegolems.client.GolemRenderer;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class SimpleGolemsForgeClient {
    private SimpleGolemsForgeClient() {}

    static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(GolemRenderer.LAYER, GolemMesh::create));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.GOLEM_ENTITY, GolemRenderer::new));
        modBus.addListener((RegisterColorHandlersEvent.Item e) -> e.register(GolemItemColours::colour, ModContent.GOLEM));
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() ->
                ItemProperties.register(ModContent.GOLEM, SimpleGolems.id("job"), (stack, level, entity, seed) -> GolemItemColours.job(stack))));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) CookieHud.tick(Minecraft.getInstance());
        });
    }
}
*///?}
