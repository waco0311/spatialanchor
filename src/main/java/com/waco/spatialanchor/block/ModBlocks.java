package com.waco.spatialanchor.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waco.spatialanchor.SpatialAnchorMod;
import com.waco.spatialanchor.item.ModItems;
import com.waco.spatialanchor.item.SpatialAnchorItem;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(SpatialAnchorMod.MOD_ID);

    // ─────────────────────────────────────────────────────────────────────
    // 空間アンカーブロック
    // ─────────────────────────────────────────────────────────────────────
    public static final DeferredBlock<SpatialAnchorBlock> SPATIAL_ANCHOR =
            BLOCKS.registerBlock(
                    "spatial_anchor",
                    SpatialAnchorBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f, 1200f)   // 爆発耐性高め（宇宙戦艦らしく）
                            .requiresCorrectToolForDrops()
            );

    // ─────────────────────────────────────────────────────────────────────
    // ブロックアイテム（Shift で詳細表示する SpatialAnchorItem を使用）
    // ─────────────────────────────────────────────────────────────────────
    public static final DeferredHolder<Item, BlockItem> SPATIAL_ANCHOR_ITEM =
            ModItems.ITEMS.register(
                    "spatial_anchor",
                    () -> new SpatialAnchorItem(SPATIAL_ANCHOR.get(), new Item.Properties())
            );

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}