package com.waco.spatialanchor.compat;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.waco.spatialanchor.SpatialAnchorMod;
import com.waco.spatialanchor.block.ModBlockEntities;

/**
 * Create の各種システムへの登録処理。
 *
 * <h2>Display Link 登録（重要: タイミングに注意）</h2>
 * <p>{@code .associate(ModBlockEntities.SPATIAL_ANCHOR.get())} を
 * static フィールド初期化時に直接呼ぶと、
 * その時点ではまだ BlockEntityType レジストリに値が無く
 * "Trying to access unbound value" で例外になる。
 * （DeferredRegister の {@code .get()} は RegisterEvent 発火後でないと使えない）</p>
 *
 * <p>正しいパターンは Create 本体の {@code AllDisplaySources} の
 * {@code COMPUTER} エントリと同じ、{@code onRegisterAfter} を使う方式:
 * レジストリの登録が完了した「後」に安全にコールバックが呼ばれる。</p>
 */
public class CreateCompatHandler {

    private CreateCompatHandler() {}

    // ── Display Source（onRegisterAfter で安全なタイミングに紐付け）─────────
    public static final RegistryEntry<DisplaySource, SpatialAnchorDisplaySource> SPATIAL_ANCHOR_STATUS =
            SpatialAnchorRegistrate.REGISTRATE
                    .displaySource("spatial_anchor_status", SpatialAnchorDisplaySource::new)
                    .onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source -> {
                        // このコールバックの時点では BLOCK_ENTITY_TYPE レジストリが
                        // 確定済みなので、安全に .get() できる。
                        BlockEntityType<?> type = ModBlockEntities.SPATIAL_ANCHOR.get();
                        DisplaySource.BY_BLOCK_ENTITY.add(type, source);
                        SpatialAnchorMod.LOGGER.debug(
                                "[SpatialAnchor] Display source associated with block entity type.");
                    })
                    .register();

    public static void registerStressEntries() {
        // KineticBlockEntity のオーバーライドで自動登録されるため追加不要
        SpatialAnchorMod.LOGGER.debug("[SpatialAnchor] Create stress entries registered.");
    }

    /**
     * クラスロードをトリガーするための no-op。
     * Mod のコンストラクタで一度呼ぶことで static フィールドの登録を確実に発火させる。
     */
    public static void registerDisplaySources() {
        SpatialAnchorMod.LOGGER.debug(
                "[SpatialAnchor] Display source 'spatial_anchor_status' class-loaded (id={}).",
                SPATIAL_ANCHOR_STATUS.getId());
    }
}