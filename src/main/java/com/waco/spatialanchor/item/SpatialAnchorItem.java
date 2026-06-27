package com.waco.spatialanchor.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 空間アンカーの BlockItem。
 *
 * <p>Shift 長押しで詳細説明を表示する。
 * 説明文は言語ファイル（lang）の以下のキーから取得する:</p>
 * <ul>
 *   <li>{@code item.spatialanchor.spatial_anchor.tooltip.hold}  — 通常時の「Shiftで詳細」案内</li>
 *   <li>{@code item.spatialanchor.spatial_anchor.tooltip.line1〜5} — Shift押下時の各行</li>
 * </ul>
 */
public class SpatialAnchorItem extends BlockItem {

    public SpatialAnchorItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack,
                                Item.TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            // Shift 押下時: 詳細説明（薄灰色）
            tooltip.add(Component.translatable("item.spatialanchor.spatial_anchor.tooltip.line1")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.spatialanchor.spatial_anchor.tooltip.line2")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.spatialanchor.spatial_anchor.tooltip.line3")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.spatialanchor.spatial_anchor.tooltip.line4")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.spatialanchor.spatial_anchor.tooltip.line5")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            // 通常時: 「Shiftで詳細」案内（濃灰色＋斜体）
            tooltip.add(Component.translatable("item.spatialanchor.spatial_anchor.tooltip.hold")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}