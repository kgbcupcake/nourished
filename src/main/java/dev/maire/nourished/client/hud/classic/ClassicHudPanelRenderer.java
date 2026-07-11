package dev.maire.nourished.client.hud.classic;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.marie.framework.client.MarieClientCache;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.client.hud.dynamic.layout.HudLayout;
import dev.maire.nourished.client.hud.dynamic.visibility.HudVisibility;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.marie.framework.tracking.TrackingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Map;

/**
 * Pre-MarieUI HUD renderer, restored verbatim from before the MarieUI-only collapse (see
 * {@code git show 57dc304:.../hud/HudPanelRenderer.java}) with only the package/imports adjusted
 * to compile against today's {@code HudLayout}/{@code HudDrawHelpers}/{@code HudVisibility}, which
 * are otherwise unchanged since then.
 */
public final class ClassicHudPanelRenderer {

    private ClassicHudPanelRenderer() {}

    public static void drawPanel(
            GuiGraphics g,
            Minecraft mc,
            TrackingData data,
            List<String> keys,
            HudLayout.Layout layout,
            int panelX,
            int panelY,
            Map<String, Float> displayValues
    ) {
        NourishedClientConfig cc = NourishedClientConfig.get();
        double bgOpacity = cc.hudBackgroundOpacity();
        if (bgOpacity > 0d) {
            HudDrawHelpers.drawRoundedRect(
                    g,
                    panelX,
                    panelY,
                    layout.panelW(),
                    layout.panelH(),
                    2,
                    HudDrawHelpers.panelColor(bgOpacity)
            );
        }
        if (layout.verticalLayout()) {
            drawVerticalColumns(g, mc, data, keys, layout, panelX, panelY, displayValues, cc);
        } else {
            drawHorizontalRows(g, mc, data, keys, layout, panelX, panelY, displayValues, cc);
        }
    }

    private static void drawVerticalColumns(
            GuiGraphics g,
            Minecraft mc,
            TrackingData data,
            List<String> keys,
            HudLayout.Layout layout,
            int panelX,
            int panelY,
            Map<String, Float> displayValues,
            NourishedClientConfig cc
    ) {
        int columnGap = Math.max(2, (int) Math.round(HudDrawHelpers.VERTICAL_COLUMN_GAP * layout.scale()));
        int contentX = panelX + layout.scaledPad();
        int pctH = (int) Math.ceil(9 * layout.labelScale());
        int barTop = panelY + layout.scaledPad() + pctH + 2;
        int labelY = barTop + layout.verticalBarH() + 2;
        int pctY = panelY + layout.scaledPad();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            float displayPct = displayValues.getOrDefault(key, 0f);
            float truePct = data.values.getOrDefault(key, 0f);
            boolean dimRow = HudVisibility.dimZeroRow(truePct, cc);
            if (dimRow) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f);
            }
            int columnX = contentX + i * (layout.verticalColumnW() + columnGap);
            int barX = columnX + (layout.verticalColumnW() - layout.verticalBarW()) / 2;
            String label = HudDrawHelpers.nutrientLabel(key);
            int labelSw = (int) Math.ceil(mc.font.width(label) * layout.labelScale());
            int labelX = columnX + (layout.verticalColumnW() - labelSw) / 2;
            int pct = Math.round(truePct * 100f);
            String pctText = pct + "%";
            int pctSw = (int) Math.ceil(mc.font.width(pctText) * layout.labelScale());
            int pctX = columnX + (layout.verticalColumnW() - pctSw) / 2;
            HudDrawHelpers.drawScaledLabel(
                    g,
                    mc,
                    pctText,
                    pctX,
                    pctY,
                    HudDrawHelpers.pctColor(key, truePct),
                    layout.labelScale()
            );
            HudDrawHelpers.drawRoundedVerticalBar(
                    g,
                    barX,
                    barTop,
                    layout.verticalBarW(),
                    layout.verticalBarH(),
                    displayPct,
                    HudDrawHelpers.barBackgroundColor(),
                    HudDrawHelpers.barFillColor(key, truePct)
            );
            float flash = MarieClientCache.flashAlpha(key);
            if (flash > 0f) {
                int a = (int) (flash * 80);
                int flashColor = (a << 24) | 0xFFFFFF;
                g.fill(
                        barX,
                        barTop,
                        barX + layout.verticalBarW(),
                        barTop + layout.verticalBarH(),
                        flashColor
                );
            }
            HudDrawHelpers.drawScaledLabel(
                    g,
                    mc,
                    label,
                    labelX,
                    labelY,
                    HudDrawHelpers.labelColor(),
                    layout.labelScale()
            );
            if (dimRow) {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
        }
    }

    private static void drawHorizontalRows(
            GuiGraphics g,
            Minecraft mc,
            TrackingData data,
            List<String> keys,
            HudLayout.Layout layout,
            int panelX,
            int panelY,
            Map<String, Float> displayValues,
            NourishedClientConfig cc
    ) {
        int contentX = panelX + layout.scaledPad();
        int y = panelY + layout.scaledPad();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            float displayPct = displayValues.getOrDefault(key, 0f);
            float truePct = data.values.getOrDefault(key, 0f);
            boolean dimRow = HudVisibility.dimZeroRow(truePct, cc);
            if (dimRow) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f);
            }
            int rowH = layout.rowH();
            int rowCenterY = y + rowH / 2;
            int iconSize = layout.iconSize();
            String label = HudDrawHelpers.nutrientLabel(key);
            int labelY = rowCenterY - (int) Math.ceil(9 * layout.labelScale()) / 2;
            int labelSw = (int) Math.ceil(mc.font.width(label) * layout.labelScale());
            int iconX = contentX;
            int labelX = contentX + iconSize + HudDrawHelpers.ICON_LABEL_GAP;
            int barX = labelX + layout.maxLabelSw() + HudDrawHelpers.LABEL_BAR_GAP;
            HudDrawHelpers.renderIcon(g, key, iconX, rowCenterY - iconSize / 2, iconSize);
            HudDrawHelpers.drawScaledLabel(g, mc, label, labelX, labelY, HudDrawHelpers.labelColor(), layout.labelScale());
            int barY = rowCenterY - HudDrawHelpers.BAR_H / 2;
            HudDrawHelpers.drawRoundedBar(
                    g,
                    barX,
                    barY,
                    layout.barW(),
                    HudDrawHelpers.BAR_H,
                    displayPct,
                    HudDrawHelpers.barBackgroundColor(),
                    HudDrawHelpers.barFillColor(key, truePct)
            );
            float flash = MarieClientCache.flashAlpha(key);
            if (flash > 0f) {
                int a = (int) (flash * 80);
                int flashColor = (a << 24) | 0xFFFFFF;
                g.fill(barX, barY, barX + layout.barW(), barY + HudDrawHelpers.BAR_H, flashColor);
            }
            int pct = Math.round(truePct * 100f);
            int pctX = barX + layout.barW() + HudDrawHelpers.BAR_PCT_GAP;
            HudDrawHelpers.drawScaledLabel(
                    g,
                    mc,
                    pct + "%",
                    pctX,
                    labelY,
                    HudDrawHelpers.pctColor(key, truePct),
                    layout.labelScale()
            );
            if (dimRow) {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
            y += rowH;
            if (i < keys.size() - 1) {
                y += HudDrawHelpers.ROW_GAP;
            }
        }
    }
}
