package com.hydradragon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.GhastRenderer;

public class HydraDragonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Stage 1: reuse the Ghast look (scaled up). Stage 2 replaces this with a real three-headed model.
        EntityRendererRegistry.register(HydraDragonMod.HYDRA_DRAGON, GhastRenderer::new);
    }
}
