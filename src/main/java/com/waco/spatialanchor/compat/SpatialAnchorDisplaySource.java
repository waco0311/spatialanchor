package com.waco.spatialanchor.compat;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.waco.spatialanchor.block.SpatialAnchorBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;

/**
 * Create Display Link 用のデータソース（1項目 = 1ソース）。
 *
 * <p>Display Link の UI で以下の3つから選択できる:</p>
 * <ul>
 *   <li>{@link Kind#STATUS}   — online / offline</li>
 *   <li>{@link Kind#POSITION} — アンカーブロックの現在ワールド座標 "x y z"</li>
 *   <li>{@link Kind#WEIGHT}   — 船重量</li>
 * </ul>
 *
 * <p>{@link SingleLineDisplaySource} を継承しているので、ラベル付けや
 * Nixie管・フラップディスプレイにもそのまま対応する。</p>
 */
public class SpatialAnchorDisplaySource extends SingleLineDisplaySource {

    public enum Kind { STATUS, POSITION, WEIGHT }

    private static final String ONLINE  = "online";
    private static final String OFFLINE = "offline";
    private static final String NO_DATA = "---";

    private final Kind kind;

    public SpatialAnchorDisplaySource(Kind kind) {
        this.kind = kind;
    }

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof SpatialAnchorBlockEntity anchor)) {
            return Component.literal(kind == Kind.STATUS ? OFFLINE : NO_DATA);
        }

        return switch (kind) {
            case STATUS -> Component.literal(anchor.isActive() ? ONLINE : OFFLINE);
            case POSITION -> {
                Vec3 pos = anchor.getCurrentWorldPos();
                yield Component.literal(pos == null ? NO_DATA : formatVec3(pos));
            }
            case WEIGHT -> Component.literal(String.valueOf(Math.round(anchor.getShipWeight())));
        };
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    /** 座標は頻繁に変わりうるので 1 秒ごとに更新。 */
    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }

    /** Vec3 を "x y z" 形式に変換（小数点以下切り捨て）。 */
    private static String formatVec3(Vec3 pos) {
        return String.format("%d %d %d",
                (int) Math.floor(pos.x),
                (int) Math.floor(pos.y),
                (int) Math.floor(pos.z));
    }
}
