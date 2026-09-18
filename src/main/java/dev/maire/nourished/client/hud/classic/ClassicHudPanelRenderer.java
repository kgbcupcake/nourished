package dev.maire.nourished.client.hud.classic;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.client.hud.dynamic.edit.HudEditTarget;
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
 * are otherwise unchanged since then, plus {@code contentScale}/{@code pad} below so this renderer
 * also honors the Nutrient HUD's edit-mode Text Scale/Padding sliders ({@link HudEditTarget}) the
 * same way {@code NutrientPanelContainer}/{@code NutrientBarComponent} do for non-classic mode —
 * previously those sliders silently had no effect here.
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
        // Same "user's persisted per-panel adjustment alone, box geometry untouched" split
        // NutrientPanelContainer/NutrientBarComponent apply for non-classic mode: layout's own
        // scale/scaledPad (cc.hudScale()-derived) still drives row/bar/panel geometry below, but
        // actual text/icon render size and content padding come from these instead.
        float contentScale = ContentScaleController.resolveContentScale(HudEditTarget.persistedContentScale());
        int pad = Math.round(ContentScaleController.resolvePadding(HudDrawHelpers.PANEL_PAD * HudEditTarget.persistedPaddingScale()));
        // Clipped to the panel's own bounds — a defensive backstop against contentOffsetX/Y (the
        // "Move Text and Icons" toggle) pushing content outside the panel: HudEditTarget clamps that
        // offset already, but without this, any drift would render content fully detached from the
        // panel background rather than simply getting cut off at its edge, same as
        // CalorieHudScreen/ActivityLogHudPanel's own pushClip/popClip around their row drawing.
        g.enableScissor(panelX, panelY, panelX + layout.panelW(), panelY + layout.panelH());
        try {
            if (layout.verticalLayout()) {
                drawVerticalColumns(g, mc, data, keys, layout, panelX, panelY, displayValues, cc, contentScale, pad);
            } else {
                drawHorizontalRows(g, mc, data, keys, layout, panelX, panelY, displayValues, cc, contentScale, pad);
            }
        } finally {
            g.disableScissor();
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
            NourishedClientConfig cc,
            float contentScale,
            int pad
    ) {
        int columnGap = Math.max(2, (int) Math.round(HudDrawHelpers.VERTICAL_COLUMN_GAP * layout.scale()));
        int contentX = panelX + pad + layout.leftMargin() + layout.contentOffsetX();
        int pctH = (int) Math.ceil(9 * contentScale);
        int barTop = panelY + pad + layout.contentOffsetY() + pctH + 2;
        int labelY = barTop + layout.verticalBarH() + 2;
        int pctY = panelY + pad + layout.contentOffsetY();
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
            int labelSw = (int) Math.ceil(mc.font.width(label) * contentScale);
            int labelX = columnX + (layout.verticalColumnW() - labelSw) / 2;
            int pct = Math.round(truePct * 100f);
            String pctText = pct + "%";
            int pctSw = (int) Math.ceil(mc.font.width(pctText) * contentScale);
            int pctX = columnX + (layout.verticalColumnW() - pctSw) / 2;
            HudDrawHelpers.drawScaledLabel(
                    g,
                    mc,
                    pctText,
                    pctX,
                    pctY,
                    HudDrawHelpers.pctColor(key, truePct),
                    contentScale
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
                    contentScale
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
            NourishedClientConfig cc,
            float contentScale,
            int pad
    ) {
        int contentX = panelX + pad + layout.leftMargin() + layout.contentOffsetX();
        int y = panelY + pad + layout.contentOffsetY();
        // Native item size (16px) times the user's persisted content-scale adjustment alone — same
        // "box geometry (layout.iconSize(), rowH) plays no part in it" split NutrientBarComponent
        // uses for its own icon (see that class's own contentScale field javadoc).
        int iconSize = Math.round(16 * contentScale);
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
            String label = HudDrawHelpers.nutrientLabel(key);
            int labelY = rowCenterY - (int) Math.ceil(9 * contentScale) / 2;
            int labelSw = (int) Math.ceil(mc.font.width(label) * contentScale);
            int iconX = contentX;
            int labelX = contentX + iconSize + HudDrawHelpers.ICON_LABEL_GAP;
            int barX = labelX + layout.maxLabelSw() + HudDrawHelpers.LABEL_BAR_GAP;
            HudDrawHelpers.renderIcon(g, key, iconX, rowCenterY - iconSize / 2, iconSize);
            HudDrawHelpers.drawScaledLabel(g, mc, label, labelX, labelY, HudDrawHelpers.labelColor(), contentScale);
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
                    contentScale
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
