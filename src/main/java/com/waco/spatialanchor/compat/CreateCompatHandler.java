package com.waco.spatialanchor.compat;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.waco.spatialanchor.SpatialAnchorMod;
import com.waco.spatialanchor.block.ModBlockEntities;

/**
 * Create の各種システムへの登録処理。
 *
 * <h2>Display Link 登録</h2>
 * <p>{@link DisplaySource#BY_BLOCK_ENTITY} に BlockEntityType → Source を追加するだけ。
 * CreateRegistrate や AllDisplaySources への直接アクセスは不要。</p>
 */
public class CreateCompatHandler {

    private CreateCompatHandler() {}

    public static void registerStressEntries() {
        // KineticBlockEntity のオーバーライドで自動登録されるため追加不要
        SpatialAnchorMod.LOGGER.info("[SpatialAnchor] Create stress entries registered.");
    }

    /**
     * Display Source を登録する。
     * FMLCommonSetupEvent の enqueueWork() 内で呼ぶこと。
     *
     * <p>登録方法: {@link DisplaySource#BY_BLOCK_ENTITY} に直接 add するだけ。
     * これで Display Link がこのブロックに対して SpatialAnchorDisplaySource を使うようになる。</p>
     */
    public static void registerDisplaySources() {
        // Display Link 連携は Registrate 導入後に有効化する予定。
        // DisplaySource.BY_BLOCK_ENTITY.add() だけでは getId() が null になり
        // クラッシュするため、現状は登録しない。
        //
        // DisplaySource.BY_BLOCK_ENTITY.add(
        //     ModBlockEntities.SPATIAL_ANCHOR.get(),
        //     new SpatialAnchorDisplaySource()
        // );

        SpatialAnchorMod.LOGGER.info("[SpatialAnchor] Display source registration skipped (pending Registrate).");
    }
}
