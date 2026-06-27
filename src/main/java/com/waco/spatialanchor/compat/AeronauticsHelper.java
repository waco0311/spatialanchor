package com.waco.spatialanchor.compat;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.waco.spatialanchor.SpatialAnchorMod;

/**
 * Sable / Create Aeronautics の物理 API ラッパー。
 *
 * <h2>固定方式（FixedConstraint）</h2>
 * <p>Aeronautics 公式の Physics Staff と同じ方式。
 * 力（インパルス）ではなく、Sable の物理拘束 {@link FixedConstraintHandle} を使う。</p>
 *
 * <p>現在の位置・回転中心・向きを {@link FixedConstraintConfiguration} に渡して
 * 拘束を作ると、その姿勢でガッチリ固定される（角度含む完全固定）。</p>
 *
 * <ul>
 *   <li>固定: {@link #createAnchorConstraint} で拘束を作りハンドルを返す</li>
 *   <li>解除: {@link FixedConstraintHandle#remove()} を呼ぶ</li>
 * </ul>
 */
public class AeronauticsHelper {

    private AeronauticsHelper() {}

    // ═══════════════════════════════════════════════════════════════════════
    // SubLevel 取得
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 指定座標を管理している ServerSubLevel を取得する。
     *
     * @return ServerSubLevel、船上でなければ null
     */
    public static ServerSubLevel getServerSubLevel(Level level, BlockPos pos) {
        try {
            var access = SableCompanion.INSTANCE.getContaining(level, Vec3.atCenterOf(pos));
            if (access instanceof ServerSubLevel serverSubLevel) {
                return serverSubLevel;
            }
            return null;
        } catch (Exception e) {
            SpatialAnchorMod.LOGGER.error("[AeronauticsHelper] getServerSubLevel() error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 指定座標が船（SubLevel）上にあるか確認。
     */
    public static boolean isOnShip(Level level, BlockPos pos) {
        return getServerSubLevel(level, pos) != null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 座標取得（Display Link 用）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ブロックのワールド座標を返す（Display Link 表示用）。
     */
    public static Vec3 getBlockWorldPos(Level level, BlockPos blockPos) {
        Vec3 localPos = Vec3.atCenterOf(blockPos);
        try {
            return SableCompanion.INSTANCE.projectOutOfSubLevel(level, localPos);
        } catch (Exception e) {
            SpatialAnchorMod.LOGGER.error("[AeronauticsHelper] getBlockWorldPos() error: {}", e.getMessage());
            return localPos;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 質量取得（必要応力の計算用）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 船の総質量(kg)を返す。
     *
     * @return 質量(kg)、取得失敗時は 0
     */
    public static float getShipWeight(Level level, BlockPos anchorPos) {
        ServerSubLevel subLevel = getServerSubLevel(level, anchorPos);
        if (subLevel == null) return 0f;
        try {
            return (float) subLevel.getMassTracker().getMass();
        } catch (Exception e) {
            SpatialAnchorMod.LOGGER.error("[AeronauticsHelper] getShipWeight() error: {}", e.getMessage());
            return 0f;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // アンカー固定（FixedConstraint）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 船を現在の姿勢で完全固定する拘束を作成する。
     *
     * <p>Aeronautics の Physics Staff と同じ方式。
     * 現在の位置・回転中心・向きで {@link FixedConstraintConfiguration} を作り、
     * 物理パイプラインに拘束として追加する。</p>
     *
     * @return 固定拘束のハンドル（解除時に {@link FixedConstraintHandle#remove()} を呼ぶ）、
     *         失敗時は null
     */
    public static FixedConstraintHandle createAnchorConstraint(Level level, BlockPos anchorPos) {
        if (!(level instanceof ServerLevel)) return null;

        ServerSubLevel subLevel = getServerSubLevel(level, anchorPos);
        if (subLevel == null) {
            SpatialAnchorMod.LOGGER.debug("[AeronauticsHelper] Not on a sub-level, cannot anchor.");
            return null;
        }

        try {
            ServerSubLevelContainer container =
                    (ServerSubLevelContainer) SubLevelContainer.getContainer((ServerLevel) level);
            if (container == null) return null;

            SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
            PhysicsPipeline pipeline = physicsSystem.getPipeline();

            // 現在の姿勢（位置・回転中心・向き）で固定拘束を作成
            FixedConstraintHandle handle = pipeline.addConstraint(null, subLevel,
                    new FixedConstraintConfiguration(
                            subLevel.logicalPose().position(),
                            subLevel.logicalPose().rotationPoint(),
                            subLevel.logicalPose().orientation()
                    ));

            SpatialAnchorMod.LOGGER.info("[AeronauticsHelper] Anchor constraint created at {}", anchorPos);
            return handle;

        } catch (Exception e) {
            SpatialAnchorMod.LOGGER.error("[AeronauticsHelper] createAnchorConstraint() error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 固定拘束を解除する。
     */
    public static void removeAnchorConstraint(FixedConstraintHandle handle) {
        if (handle == null) return;
        try {
            handle.remove();
            SpatialAnchorMod.LOGGER.debug("[AeronauticsHelper] Anchor constraint removed.");
        } catch (Exception e) {
            SpatialAnchorMod.LOGGER.error("[AeronauticsHelper] removeAnchorConstraint() error: {}", e.getMessage());
        }
    }
}
