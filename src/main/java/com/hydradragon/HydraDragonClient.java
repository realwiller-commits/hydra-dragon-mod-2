package com.hydradragon;

import com.hydradragon.client.HydraDragonModelLayers;
import com.hydradragon.client.HydraDragonRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class HydraDragonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HydraDragonModelLayers.register();
        EntityRenderers.register(HydraDragonMod.HYDRA_DRAGON, HydraDragonRenderer::new);
    }
}
