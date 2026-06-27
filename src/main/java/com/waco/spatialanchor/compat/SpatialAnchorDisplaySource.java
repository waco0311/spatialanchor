package com.waco.spatialanchor.compat;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import com.waco.spatialanchor.block.SpatialAnchorBlockEntity;

import java.util.List;

/**
 * Create Display Link 用のデータソース。
 *
 * <p>登録: {@link CreateCompatHandler#registerDisplaySources()} で
 * {@code DisplaySource.BY_BLOCK_ENTITY.add()} を呼ぶ。</p>
 *
 * <p>出力フォーマット (3行):</p>
 * <pre>
 *   行1: online  または  offline
 *   行2: アンカー座標  例: -128 64 300    (停止中は ---)
 *   行3: 現在座標      例: -127 64 299    (停止中は ---)
 * </pre>
 */
public class SpatialAnchorDisplaySource extends DisplaySource {

    private static final String ONLINE  = "online";
    private static final String OFFLINE = "offline";
    private static final String NO_DATA = "---";

    // ─────────────────────────────────────────────────────────────────────
    // DisplaySource 実装
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        BlockEntity be = context.getSourceBlockEntity();

        if (!(be instanceof SpatialAnchorBlockEntity anchor)) {
            return offlineResponse();
        }

        return List.of(
            buildStatusLine(anchor),
            buildAnchorPosLine(anchor),
            buildCurrentPosLine(anchor)
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // 各行のビルダー
    // ─────────────────────────────────────────────────────────────────────

    /** 行1: online / offline */
    private MutableComponent buildStatusLine(SpatialAnchorBlockEntity anchor) {
        return Component.literal(anchor.isActive() ? ONLINE : OFFLINE);

    }

    /**
     * 行2: アンカー固定座標
     * <pre>
     *   稼働中: -128 64 300
     *   停止中: ---
     * </pre>
     */
    private MutableComponent buildAnchorPosLine(SpatialAnchorBlockEntity anchor) {
        if (!anchor.isActive()) return Component.literal(NO_DATA);
        Vec3 pos = anchor.getAnchorPos();
        if (pos == null)  return Component.literal(NO_DATA);
        return Component.literal(formatVec3(pos));
    }

    /**
     * 行3: 現在座標（船の現在ワールド位置）
     * <pre>
     *   稼働中: -127 64 299
     *   停止中: ---
     * </pre>
     */
    private MutableComponent buildCurrentPosLine(SpatialAnchorBlockEntity anchor) {
        if (!anchor.isActive()) return Component.literal(NO_DATA);
        Vec3 pos = anchor.getCurrentWorldPos();
        if (pos == null)  return Component.literal(NO_DATA);
        return Component.literal(formatVec3(pos));
    }

    // ─────────────────────────────────────────────────────────────────────
    // ユーティリティ
    // ─────────────────────────────────────────────────────────────────────

    /** Vec3 を "x y z" 形式の文字列に変換（小数点以下切り捨て）。 */
    private static String formatVec3(Vec3 pos) {
        return String.format("%d %d %d",
            (int) Math.floor(pos.x),
            (int) Math.floor(pos.y),
            (int) Math.floor(pos.z));
    }

    private static List<MutableComponent> offlineResponse() {
        return List.of(
            Component.literal(OFFLINE),
            Component.literal(NO_DATA),
            Component.literal(NO_DATA)
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // 更新頻度
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Display Link が自動更新する間隔 (tick)。
     * デフォルト100tick(5秒)。座標は頻繁に変わりうるので短めに設定。
     */
    @Override
    public int getPassiveRefreshTicks() {
        return 20; // 1秒ごとに更新
    }
}
