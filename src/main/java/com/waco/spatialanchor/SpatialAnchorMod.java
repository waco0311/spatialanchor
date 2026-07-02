package com.waco.spatialanchor;

import com.waco.spatialanchor.compat.CreateCompatHandler;
import com.waco.spatialanchor.compat.SpatialAnchorRegistrate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import com.waco.spatialanchor.block.ModBlockEntities;
import com.waco.spatialanchor.block.ModBlocks;
import com.waco.spatialanchor.item.ModItems;
import com.waco.spatialanchor.config.SpatialAnchorConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SpatialAnchorMod.MOD_ID)
public class SpatialAnchorMod {

    public static final String MOD_ID = "spatialanchor";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public SpatialAnchorMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SpatialAnchorConfig.SERVER_SPEC);

        // Registrate のイベントリスナーを登録。
        // .register() で予約された内容を実際の RegisterEvent に紐付ける。
        SpatialAnchorRegistrate.REGISTRATE.registerEventListeners(modEventBus);

        // ブロック/アイテム/BlockEntity を先に登録
        // （DisplaySource の associate() が BlockEntityType を参照するため、順序が重要）
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // 重要: CreateCompatHandler の static フィールド（DisplaySource登録）を
        // ここで強制的にクラスロードさせる。
        // Registrate の .register() は static初期化時に予約される必要があり、
        // FMLCommonSetupEvent 等の遅いタイミングで初めて触れると
        // 例外は出ないが実際にはレジストリに反映されない。
        CreateCompatHandler.registerDisplaySources();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::addCreativeTab);

        LOGGER.info("[SpatialAnchor] Mod initialized.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CreateCompatHandler.registerStressEntries();
            LOGGER.info("[SpatialAnchor] Common setup complete.");
        });
    }

    private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.SPATIAL_ANCHOR_ITEM.get());
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("[SpatialAnchor] Client setup complete.");
    }
}