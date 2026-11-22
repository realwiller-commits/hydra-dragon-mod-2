package com.hydradragon.client;

import com.hydradragon.HydraDragonMod;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class HydraDragonModelLayers {
    public static final ModelLayerLocation HYDRA_DRAGON = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(HydraDragonMod.MOD_ID, "hydra_dragon"), "main");

    private HydraDragonModelLayers() {
    }

    public static void register() {
        ModelLayerRegistry.registerModelLayer(HYDRA_DRAGON, HydraDragonModel::createLayer);
    }
}
