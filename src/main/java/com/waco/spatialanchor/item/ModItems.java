package com.waco.spatialanchor.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import com.waco.spatialanchor.SpatialAnchorMod;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, SpatialAnchorMod.MOD_ID);

    // BlockItem は ModBlocks 側で登録するため、ここでは追加アイテムのみ
    // （今回は追加アイテムなし）

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
