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
    // 稼働状態 / 座標 / 重量 の3ソースを個別に登録する。
    public static final RegistryEntry<DisplaySource, SpatialAnchorDisplaySource> STATUS =
            registerSource("spatial_anchor_status", SpatialAnchorDisplaySource.Kind.STATUS);
    public static final RegistryEntry<DisplaySource, SpatialAnchorDisplaySource> POSITION =
            registerSource("spatial_anchor_position", SpatialAnchorDisplaySource.Kind.POSITION);
    public static final RegistryEntry<DisplaySource, SpatialAnchorDisplaySource> WEIGHT =
            registerSource("spatial_anchor_weight", SpatialAnchorDisplaySource.Kind.WEIGHT);

    private static RegistryEntry<DisplaySource, SpatialAnchorDisplaySource> registerSource(
            String name, SpatialAnchorDisplaySource.Kind kind) {
        return SpatialAnchorRegistrate.REGISTRATE
                .displaySource(name, () -> new SpatialAnchorDisplaySource(kind))
                .onRegisterAfter(Registries.BLOCK_ENTITY_TYPE, source -> {
                    // この時点で BLOCK_ENTITY_TYPE レジストリは確定済みなので .get() できる
                    BlockEntityType<?> type = ModBlockEntities.SPATIAL_ANCHOR.get();
                    DisplaySource.BY_BLOCK_ENTITY.add(type, source);
                })
                .register();
    }

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
                "[SpatialAnchor] Display sources class-loaded ({}, {}, {}).",
                STATUS.getId(), POSITION.getId(), WEIGHT.getId());
    }
}