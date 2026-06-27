package com.waco.spatialanchor.block;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import com.waco.spatialanchor.SpatialAnchorMod;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SpatialAnchorMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpatialAnchorBlockEntity>> SPATIAL_ANCHOR =
            BLOCK_ENTITIES.register(
                    "spatial_anchor",
                    () -> BlockEntityType.Builder
                            .of(SpatialAnchorBlockEntity::new, ModBlocks.SPATIAL_ANCHOR.get())
                            .build(null)
            );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
