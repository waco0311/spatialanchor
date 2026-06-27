package com.waco.spatialanchor.block;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.waco.spatialanchor.SpatialAnchorMod;
import com.waco.spatialanchor.compat.AeronauticsHelper;
import com.waco.spatialanchor.config.SpatialAnchorConfig;

/**
 * 空間アンカーの BlockEntity
 *
 * <p>動作フロー:</p>
 * <ol>
 *   <li>回転（応力）供給開始 → FixedConstraint で船を現在姿勢に完全固定</li>
 *   <li>応力不足 or 回転停止 → 拘束を解除して固定を外す</li>
 * </ol>
 *
 * <p>固定は Sable の物理拘束（Aeronautics の Physics Staff と同じ方式）を使うため、
 * 位置・角度ともにガッチリ固定される。毎tickの力計算は不要。</p>
 *
 * <p>必要応力 = 船重量 × weightMultiplier × stressPerWeightUnit</p>
 */
public class SpatialAnchorBlockEntity extends KineticBlockEntity {

    // ── 状態 ─────────────────────────────────────────────────────────────
    /** アンカー有効フラグ */
    private boolean active = false;

    /** 記録したワールド座標（アンカー起動時点、Display表示用） */
    private Vec3 anchorWorldPos = null;

    /** Sable の固定拘束ハンドル（解除時に remove する） */
    private FixedConstraintHandle constraintHandle = null;

    /** キャッシュ: 最後に計算した船重量 */
    private float cachedShipWeight = 0f;

    /** キャッシュ: 最後に確認したアンカー数 */

    /** 重量を再計算するまでのtick間隔 */
    private int weightRecalcTimer = 0;

    // ── コンストラクタ ────────────────────────────────────────────────────
    public SpatialAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPATIAL_ANCHOR.get(), pos, state);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 毎Tick 処理
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) return;

        // 起動前にも重量・必要応力を把握しておく（定期再計算）
        if (++weightRecalcTimer >= SpatialAnchorConfig.getWeightRecalcInterval()) {
            weightRecalcTimer = 0;
            recalculateCache();
        }

        // 起動条件チェック
        // 条件1: 回転速度が最低値（64 RPM）以上
        // 条件2: 応力が足りている（過負荷でない）
        float speed = Math.abs(getSpeed());
        boolean speedOk  = speed >= SpatialAnchorConfig.getMinSpeed();
        boolean stressOk = !isOverStressed();
        boolean canRun = speedOk && stressOk;

        if (canRun && !active) {
            tryActivate();
        } else if (!canRun && active) {
            // 速度不足 or 応力不足 → 解除
            if (!stressOk) broadcastStressInsufficient();
            deactivate();
            return;
        }

        if (!active) return;

        // 拘束が無効化されていたら作り直す（船の再アセンブリ等）
        if (constraintHandle == null || !constraintHandle.isValid()) {
            reapplyConstraint();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // アクティブ化 / 非アクティブ化
    // ═══════════════════════════════════════════════════════════════════════

    private void tryActivate() {
        if (!AeronauticsHelper.isOnShip(level, worldPosition)) {
            SpatialAnchorMod.LOGGER.debug(
                    "[SpatialAnchor] Not on a ship at {}. Cannot activate.", worldPosition);
            return;
        }

        // 記録座標（Display表示用）
        anchorWorldPos = AeronauticsHelper.getBlockWorldPos(level, worldPosition);
        if (anchorWorldPos == null) {
            anchorWorldPos = Vec3.atCenterOf(worldPosition);
        }

        recalculateCache();

        // Sable の固定拘束を作成（位置・角度を完全固定）
        constraintHandle = AeronauticsHelper.createAnchorConstraint(level, worldPosition);
        if (constraintHandle == null) {
            SpatialAnchorMod.LOGGER.warn(
                    "[SpatialAnchor] Failed to create constraint at {}.", worldPosition);
            anchorWorldPos = null;
            return;
        }

        active = true;
        level.setBlock(worldPosition,
                getBlockState().setValue(SpatialAnchorBlock.ACTIVE, true), 3);

        // 起動音（リスポーンアンカー充電音）
        level.playSound(null, worldPosition,
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS,
                1.0f, 1.0f);

        SpatialAnchorMod.LOGGER.info(
                "[SpatialAnchor] Activated at {}. Anchor pos: {}", worldPosition, anchorWorldPos);
        setChanged();
    }

    public void deactivate() {
        if (!active) return;

        // 拘束を解除
        AeronauticsHelper.removeAnchorConstraint(constraintHandle);
        constraintHandle = null;

        active = false;
        anchorWorldPos = null;
        cachedShipWeight = 0f;
        weightRecalcTimer = 0;

        if (level != null && !level.isClientSide) {
            level.setBlock(worldPosition,
                    getBlockState().setValue(SpatialAnchorBlock.ACTIVE, false), 3);

            // 停止音（火が消える音）
            level.playSound(null, worldPosition,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    0.8f, 1.0f);
        }

        SpatialAnchorMod.LOGGER.debug("[SpatialAnchor] Deactivated at {}.", worldPosition);
        setChanged();
    }

    /** 拘束が無効になっていた場合に作り直す。 */
    private void reapplyConstraint() {
        AeronauticsHelper.removeAnchorConstraint(constraintHandle);
        constraintHandle = AeronauticsHelper.createAnchorConstraint(level, worldPosition);
        if (constraintHandle == null) {
            SpatialAnchorMod.LOGGER.debug(
                    "[SpatialAnchor] Could not reapply constraint at {}.", worldPosition);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 応力計算
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public float calculateAddedStressCapacity() {
        return 0f;
    }

    @Override
    public float calculateStressApplied() {
        // Create がこの戻り値（impact）に |speed| を乗算して実消費を決める。
        //
        // 重要: active かどうかに関係なく常に必要応力を要求する。
        // こうしないと「起動前は消費0→過負荷判定されない→起動→過負荷→解除」
        // という点滅ループになる。常時要求することで、重い船は
        // 応力が足りなければ最初から起動できない（＝過負荷で弾かれる）。
        //
        // impact = required / speed とすることで、実消費(= impact × speed)が
        // 回転速度に関わらず required で一定になる（64〜256で消費は変わらない）。
        if (level == null) return 0f;

        float required = SpatialAnchorConfig.calcRequiredStress(cachedShipWeight);
        if (required <= 0) return 0f;

        float speed = Math.abs(getSpeed());
        if (speed < 1.0f) return 0f; // 停止時は消費なし（回っていないので固定もしない）

        float clamped = Math.min(Math.max(speed, SpatialAnchorConfig.getMinSpeed()),
                SpatialAnchorConfig.getMaxSpeed());
        return required / clamped;
    }

    private float calculateRequiredStress() {
        return SpatialAnchorConfig.calcRequiredStress(cachedShipWeight);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // キャッシュ再計算
    // ═══════════════════════════════════════════════════════════════════════

    private void recalculateCache() {
        if (level == null) return;

        float prevWeight = cachedShipWeight;

        float weight = AeronauticsHelper.getShipWeight(level, worldPosition);
        cachedShipWeight = (weight > 0) ? weight : 1f;


        // 重量が変わったら Create に応力ネットワークの更新を促す。
        // calculateStressApplied() は Create が定期的に再評価するため、
        // setChanged() で BE を dirty にしておけば次回計算で反映される。
        if (Math.abs(prevWeight - cachedShipWeight) > 0.01f) {
            setChanged();
        }

        SpatialAnchorMod.LOGGER.debug(
                "[SpatialAnchor] Cache updated: weight={}, requiredStress={}",
                cachedShipWeight, calculateRequiredStress());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UI / デバッグ
    // ═══════════════════════════════════════════════════════════════════════

    public void sendStatusMessage(Player player) {
        float required = calculateRequiredStress();

        if (!active) {
            // 非アクティブ時も必要応力と現在の状態を表示
            float speed = Math.abs(getSpeed());
            String reason;
            if (speed < SpatialAnchorConfig.getMinSpeed()) {
                reason = String.format("回転不足 (%.0f/%d RPM)", speed, SpatialAnchorConfig.getMinSpeed());
            } else if (isOverStressed()) {
                reason = "応力不足";
            } else {
                reason = "待機中";
            }
            player.sendSystemMessage(Component.literal(String.format(
                    "[SpatialAnchor] 非アクティブ (%s) | 船重量: %.0f | 必要応力: %.0f Su",
                    reason, cachedShipWeight, required)));
            return;
        }

        String pos  = anchorWorldPos != null
                ? String.format("(%.1f, %.1f, %.1f)",
                anchorWorldPos.x, anchorWorldPos.y, anchorWorldPos.z)
                : "不明";

        player.sendSystemMessage(Component.literal(String.format(
                "[SpatialAnchor] アクティブ | アンカー座標: %s | 船重量: %.0f | 必要応力: %.0f Su",
                pos, cachedShipWeight, required)));
    }

    private void broadcastStressInsufficient() {
        SpatialAnchorMod.LOGGER.warn(
                "[SpatialAnchor] Anchor at {} released due to insufficient stress!", worldPosition);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NBT シリアライズ
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    protected void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Active", active);
        if (anchorWorldPos != null) {
            tag.putDouble("AnchorX", anchorWorldPos.x);
            tag.putDouble("AnchorY", anchorWorldPos.y);
            tag.putDouble("AnchorZ", anchorWorldPos.z);
        }
        tag.putFloat("CachedWeight", cachedShipWeight);
    }

    @Override
    protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        active = tag.getBoolean("Active");
        if (tag.contains("AnchorX")) {
            anchorWorldPos = new Vec3(
                    tag.getDouble("AnchorX"),
                    tag.getDouble("AnchorY"),
                    tag.getDouble("AnchorZ")
            );
        } else {
            anchorWorldPos = null;
        }
        cachedShipWeight   = tag.getFloat("CachedWeight");
        // 注: constraintHandle は再起動後に tick() で reapplyConstraint() により復元される
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ゲッター
    // ═══════════════════════════════════════════════════════════════════════

    public boolean isActive()       { return active; }
    public Vec3 getAnchorPos()      { return anchorWorldPos; }
    public float getShipWeight()    { return cachedShipWeight; }

    /** このアンカーブロックの現在ワールド座標を返す（Display Link 用）。 */
    public Vec3 getCurrentWorldPos() {
        if (!active || level == null) return null;
        return AeronauticsHelper.getBlockWorldPos(level, worldPosition);
    }
}