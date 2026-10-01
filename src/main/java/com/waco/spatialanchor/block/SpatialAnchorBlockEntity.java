package com.waco.spatialanchor.block;

import java.util.List;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    /** キャッシュ: 船上に設置されているか（ゴーグル表示用にクライアントへ同期） */
    private boolean cachedOnShip = false;

    /** 重量を再計算するまでのtick間隔 */
    private int weightRecalcTimer = 0;

    /** 最後に応力ネットワークへ通知した impact（変化検知用） */
    private float syncedImpact = Float.NaN;

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
        // 未計算（設置直後）なら即計算して、0 Su のまま起動判定に入らないようにする
        if (cachedShipWeight <= 0f
                || ++weightRecalcTimer >= SpatialAnchorConfig.getWeightRecalcInterval()) {
            weightRecalcTimer = 0;
            recalculateCache();
        }

        // 重量・回転数の変化を応力ネットワークへ反映
        syncStressToNetwork();

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

        SpatialAnchorMod.LOGGER.debug(
                "[SpatialAnchor] Activated at {}. Anchor pos: {}", worldPosition, anchorWorldPos);
        sendData();
    }

    public void deactivate() {
        if (!active) return;

        // 拘束を解除
        AeronauticsHelper.removeAnchorConstraint(constraintHandle);
        constraintHandle = null;

        active = false;
        anchorWorldPos = null;
        // 注: cachedShipWeight は保持する（応力は非アクティブ時も常時要求するため）
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
        if (level != null && !level.isClientSide) sendData();
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
        // Create がこの戻り値（impact）に |theoreticalSpeed| を乗算して実消費を決める。
        //
        // - active に関係なく常時要求（起動→過負荷→解除の点滅ループ防止）
        // - impact = required / speed で、64〜maxSpeed の範囲では総消費が required で一定
        // - getSpeed() は過負荷時に 0 を返すため使わない（使うと過負荷⇔解消で点滅する）
        float impact = computeImpact();
        this.lastStressApplied = impact;
        return impact;
    }

    private float computeImpact() {
        if (level == null) return 0f;

        float required = SpatialAnchorConfig.calcRequiredStress(cachedShipWeight);
        if (required <= 0) return 0f;

        float speed = Math.abs(getTheoreticalSpeed());
        if (speed < 1.0f) return 0f;

        float clamped = Math.min(Math.max(speed, SpatialAnchorConfig.getMinSpeed()),
                SpatialAnchorConfig.getMaxSpeed());
        return required / clamped;
    }

    /**
     * 応力ネットワークに現在の impact を通知する。
     *
     * <p>Create の KineticNetwork はネットワーク参加時の impact をキャッシュし、
     * 以後は {@code updateStressFor()} が呼ばれない限り更新しない。
     * 参加時点では船重量が未計算（0）のため、通知しないと消費 0 Su のままになる。</p>
     */
    private void syncStressToNetwork() {
        if (!hasNetwork()) {
            syncedImpact = Float.NaN;
            return;
        }
        float impact = calculateStressApplied();
        if (!Float.isNaN(syncedImpact) && Math.abs(impact - syncedImpact) < 1e-4f) return;

        syncedImpact = impact;
        getOrCreateNetwork().updateStressFor(this, impact);
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
        boolean prevOnShip = cachedOnShip;

        cachedOnShip = AeronauticsHelper.isOnShip(level, worldPosition);
        float weight = AeronauticsHelper.getShipWeight(level, worldPosition);
        cachedShipWeight = (weight > 0) ? weight : 1f;

        // ゴーグル表示用にクライアントへ同期
        if (Math.abs(prevWeight - cachedShipWeight) > 0.01f || prevOnShip != cachedOnShip) {
            sendData();
        }

        SpatialAnchorMod.LOGGER.debug(
                "[SpatialAnchor] Cache updated: weight={}, requiredStress={}",
                cachedShipWeight, calculateRequiredStress());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ゴーグル表示
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
                .add(Component.translatable("goggles.spatialanchor.header"))
                .forGoggles(tooltip);

        // 状態
        if (active) {
            CreateLang.builder()
                    .add(Component.translatable("goggles.spatialanchor.active"))
                    .style(ChatFormatting.GREEN)
                    .forGoggles(tooltip, 1);
        } else {
            CreateLang.builder()
                    .add(Component.translatable("goggles.spatialanchor.inactive",
                            Component.translatable(getInactiveReasonKey(),
                                    String.format("%.0f", Math.abs(getSpeed())),
                                    SpatialAnchorConfig.getMinSpeed())))
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip, 1);
        }

        // アンカー座標（アクティブ時のみ）
        if (active && anchorWorldPos != null) {
            CreateLang.builder()
                    .add(Component.translatable("goggles.spatialanchor.anchor_pos",
                            String.format("%.1f, %.1f, %.1f",
                                    anchorWorldPos.x, anchorWorldPos.y, anchorWorldPos.z)))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
        }

        // 船重量・必要応力
        if (cachedOnShip) {
            CreateLang.builder()
                    .add(Component.translatable("goggles.spatialanchor.ship_weight",
                            String.format("%.0f", cachedShipWeight)))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
            CreateLang.builder()
                    .add(Component.translatable("goggles.spatialanchor.required_stress",
                            String.format("%.0f", calculateRequiredStress())))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
        }

        // Create 標準の応力インパクト表示（0 のときは何も追加されない）
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    /** 非アクティブ理由の翻訳キー（クライアント側で同期済みの値から判定）。 */
    private String getInactiveReasonKey() {
        if (!cachedOnShip) return "goggles.spatialanchor.reason.not_on_ship";
        if (isOverStressed()) return "goggles.spatialanchor.reason.overstressed";
        if (Math.abs(getSpeed()) < SpatialAnchorConfig.getMinSpeed())
            return "goggles.spatialanchor.reason.low_speed";
        return "goggles.spatialanchor.reason.standby";
    }

    private void broadcastStressInsufficient() {
        SpatialAnchorMod.LOGGER.debug(
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
        tag.putBoolean("OnShip", cachedOnShip);
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
        cachedOnShip       = tag.getBoolean("OnShip");
        // 注: constraintHandle は再起動後に tick() で reapplyConstraint() により復元される
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ゲッター
    // ═══════════════════════════════════════════════════════════════════════

    public boolean isActive()       { return active; }
    public Vec3 getAnchorPos()      { return anchorWorldPos; }
    public float getShipWeight()    { return cachedShipWeight; }

    /**
     * このアンカーブロックの現在ワールド座標を返す（Display Link 用）。
     * アクティブ状態に関わらず取得できる。level が無い場合のみ null。
     */
    public Vec3 getCurrentWorldPos() {
        if (level == null) return null;
        return AeronauticsHelper.getBlockWorldPos(level, worldPosition);
    }
}