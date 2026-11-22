package com.hydradragon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class HydraDragonMod implements ModInitializer {
    public static final String MOD_ID = "hydradragon";

    public static final ResourceKey<EntityType<?>> HYDRA_DRAGON_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "hydra_dragon"));

    public static final EntityType<HydraDragon> HYDRA_DRAGON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            HYDRA_DRAGON_KEY,
            EntityType.Builder.of(HydraDragon::new, MobCategory.MONSTER)
                    .sized(4.0f, 4.0f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(HYDRA_DRAGON_KEY));

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(HYDRA_DRAGON, HydraDragon.createDragonAttributes());
    }
}
