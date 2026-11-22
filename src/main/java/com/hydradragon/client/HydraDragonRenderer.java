package com.hydradragon.client;

import com.hydradragon.HydraDragon;
import com.hydradragon.HydraDragonMod;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class HydraDragonRenderer extends MobRenderer<HydraDragon, HydraDragonRenderState, HydraDragonModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(HydraDragonMod.MOD_ID, "textures/entity/hydra_dragon.png");

    public HydraDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new HydraDragonModel(context.bakeLayer(HydraDragonModelLayers.HYDRA_DRAGON)), 1.5f);
    }

    @Override
    public HydraDragonRenderState createRenderState() {
        return new HydraDragonRenderState();
    }

    @Override
    public Identifier getTextureLocation(HydraDragonRenderState state) {
        return TEXTURE;
    }
}
