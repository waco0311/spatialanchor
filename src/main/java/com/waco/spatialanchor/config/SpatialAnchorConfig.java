package com.waco.spatialanchor.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Spatial Anchor の設定クラス。
 *
 * <p>NeoForge の ModConfigSpec を使用。
 * 設定ファイルは {@code config/spatialanchor-server.toml} に生成される。</p>
 */
public class SpatialAnchorConfig {

    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder()
                .configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER      = specPair.getLeft();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // サーバー設定クラス
    // ═══════════════════════════════════════════════════════════════════════
    public static class Server {

        // ── 応力（重量係数）─────────────────────────────────────────────────
        public final ModConfigSpec.DoubleValue weightMultiplier;
        public final ModConfigSpec.DoubleValue stressPerWeightUnit;

        // ── 起動に必要な回転速度 ─────────────────────────────────────────────
        public final ModConfigSpec.IntValue minSpeed;   // 起動下限
        public final ModConfigSpec.IntValue maxSpeed;   // 消費頭打ち上限

        // ── 重量キャッシュ更新間隔 ─────────────────────────────────────────
        public final ModConfigSpec.IntValue weightRecalcInterval;

        Server(ModConfigSpec.Builder builder) {

            // ── 応力計算 ──────────────────────────────────────────────────
            builder.comment(
                    "=== 応力計算 ===",
                    "必要応力 = 船重量 × weightMultiplier × stressPerWeightUnit"
            ).push("stress");

            weightMultiplier = builder
                    .comment(
                            "船体重量に掛ける倍率。",
                            "2.0 にすると必要応力が2倍になる（重い船ほど厳しくなる）。",
                            "デフォルト: 1.0"
                    )
                    .defineInRange("weightMultiplier", 1.0, 0.1, 100.0);

            stressPerWeightUnit = builder
                    .comment(
                            "船重量1単位あたりの基礎必要応力 (Su)。",
                            "Create の応力単位と Aeronautics の重量単位の変換係数。",
                            "デフォルト: 4.0"
                    )
                    .defineInRange("stressPerWeightUnit", 4.0, 0.1, 10000.0);

            minSpeed = builder
                    .comment(
                            "アンカー起動に必要な最低回転速度 (RPM、絶対値)。",
                            "これ未満では起動しない。デフォルト: 64"
                    )
                    .defineInRange("minSpeed", 64, 1, 256);

            maxSpeed = builder
                    .comment(
                            "応力消費が頭打ちになる回転速度 (RPM、絶対値)。",
                            "64〜この値の範囲では必要応力は一定。Create の上限は 256。",
                            "デフォルト: 256"
                    )
                    .defineInRange("maxSpeed", 256, 1, 256);

            builder.pop();

            // ── パフォーマンス ────────────────────────────────────────────
            builder.comment("=== パフォーマンス ===").push("performance");

            weightRecalcInterval = builder
                    .comment(
                            "船重量を再計算する間隔 (tick)。",
                            "小さくすると精度が上がるがサーバー負荷が増える。",
                            "デフォルト: 40 (2秒)"
                    )
                    .defineInRange("weightRecalcInterval", 40, 1, 200);

            builder.pop();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 値アクセサ（BlockEntity から呼ぶ用）
    // ═══════════════════════════════════════════════════════════════════════

    /** 必要応力を計算する（船重量 × 倍率 × 単位応力）。 */
    public static float calcRequiredStress(float shipWeight) {
        double multiplier = SERVER.weightMultiplier.get();
        double stressPerUnit = SERVER.stressPerWeightUnit.get();
        return (float) (shipWeight * multiplier * stressPerUnit);
    }

    public static int getMinSpeed()             { return SERVER.minSpeed.get(); }
    public static int getMaxSpeed()             { return SERVER.maxSpeed.get(); }
    public static int getWeightRecalcInterval() { return SERVER.weightRecalcInterval.get(); }
}