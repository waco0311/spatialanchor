package com.waco.spatialanchor;

import com.waco.spatialanchor.compat.CreateCompatHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import com.waco.spatialanchor.block.ModBlockEntities;
import com.waco.spatialanchor.block.ModBlocks;
import com.waco.spatialanchor.item.ModItems;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import com.waco.spatialanchor.config.SpatialAnchorConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;

@Mod(SpatialAnchorMod.MOD_ID)
public class SpatialAnchorMod {

    public static final String MOD_ID = "spatialanchor";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public SpatialAnchorMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SpatialAnchorConfig.SERVER_SPEC);



        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::addCreativeTab);

        LOGGER.info("[SpatialAnchor] Mod initialized.");
    }


    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CreateCompatHandler.registerStressEntries();
            CreateCompatHandler.registerDisplaySources();

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
