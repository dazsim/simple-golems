package com.workshopcraft.simplegolems.neoforge;

import com.workshopcraft.simplegolems.SimpleGolems;
import com.workshopcraft.simplegolems.client.CookieHud;
import com.workshopcraft.simplegolems.client.GolemMesh;
import com.workshopcraft.simplegolems.client.GolemRenderer;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SimpleGolems.MOD_ID, dist = Dist.CLIENT)
public class SimpleGolemsNeoForgeClient {
    public SimpleGolemsNeoForgeClient(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(GolemRenderer.LAYER, GolemMesh::create));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.GOLEM_ENTITY, GolemRenderer::new));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> CookieHud.tick(Minecraft.getInstance()));
    }
}
