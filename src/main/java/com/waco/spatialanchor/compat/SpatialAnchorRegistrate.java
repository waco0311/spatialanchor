package com.waco.spatialanchor.compat;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.waco.spatialanchor.SpatialAnchorMod;

/**
 * Display Link などの Create 専用レジストリ（Registrate 経由でのみ登録可能なもの）
 * のためだけに使う CreateRegistrate インスタンス。
 *
 * <p>ブロック/アイテム本体は引き続き {@code ModBlocks}（DeferredRegister）で管理する。
 * これは DisplaySource など、Registrate 経由の登録が必須な機能専用。</p>
 *
 * <p>背景: {@code DisplaySource.BY_BLOCK_ENTITY.add(...)} だけでは
 * 内部の {@code getId()} が null のままクラッシュする。
 * Create の {@code AllDisplaySources} は必ず
 * {@code REGISTRATE.displaySource(name, ctor).associate(beType).register()}
 * の形で登録しており、これが唯一の正しい登録経路。</p>
 */
public class SpatialAnchorRegistrate {

    public static final CreateRegistrate REGISTRATE =
            CreateRegistrate.create(SpatialAnchorMod.MOD_ID);

    private SpatialAnchorRegistrate() {}
}