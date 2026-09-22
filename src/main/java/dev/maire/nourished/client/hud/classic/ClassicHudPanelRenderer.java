package dev.maire.nourished.client.hud.classic;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.maire.nourished.client.UiStatePersistence;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.maire.nourished.client.colors.NourishedColors;
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

    /** The Nutrient HUD panel's persisted UI-state key (same string {@code HudEditTarget} keys its scale/position under). */
    private static final String PANEL_ID = "nourished.hud.panel";

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
            // Same border color/shade/opacity composition as the non-classic NutrientPanelContainer,
            // just drawn as a plain stroke (GuiGraphics has no bordered-rounded-rect primitive here).
            int borderRgb = MarieColors.shade(NourishedColors.rgb(NourishedColors.HUD_BORDER), cc.hudBorderShade());
            int borderColor = HudDrawHelpers.panelColorWithOpacity(borderRgb, bgOpacity * cc.hudBorderOpacity());
            HudDrawHelpers.drawBorder(g, panelX, panelY, layout.panelW(), layout.panelH(), 1, borderColor);
        }
        // Same "user's persisted per-panel adjustment alone, box geometry untouched" split
        // NutrientPanelContainer/NutrientBarComponent apply for non-classic mode: layout's own
        // scale/scaledPad (cc.hudScale()-derived) still drives row/bar/panel geometry below, but
        // actual text/icon render size and content padding come from these instead.
        float contentScale = ContentScaleController.resolveContentScale(HudEditTarget.persistedContentScale());
        int pad = Math.round(ContentScaleController.resolvePadding(HudDrawHelpers.PANEL_PAD * HudEditTarget.persistedPaddingScale()));
        // Read once per panel draw, not per row: the "Hide Icons"/"Hide Bars"/"Hide Text" toggles apply
        // uniformly to every row, same as the dynamic renderer (NutrientBarComponent). This renderer
        // predates the toggles (see class javadoc) and, unlike the dynamic renderer, never checked any
        // of them at all — the icon guard below is a fix, not a new feature, alongside the two new ones.
        boolean hideIcons = MarieModuleSettings.isIconsHidden(UiStatePersistence.get(), PANEL_ID);
        boolean hideBars = MarieModuleSettings.isBarsHidden(UiStatePersistence.get(), PANEL_ID);
        boolean hideText = MarieModuleSettings.isTextHidden(UiStatePersistence.get(), PANEL_ID);
        // Clipped to the panel's own bounds — a defensive backstop against contentOffsetX/Y (the
        // "Move Text and Icons" toggle) pushing content outside the panel: HudEditTarget clamps that
        // offset already, but without this, any drift would render content fully detached from the
        // panel background rather than simply getting cut off at its edge, same as
        // CalorieHudScreen/ActivityLogHudPanel's own pushClip/popClip around their row drawing.
        g.enableScissor(panelX, panelY, panelX + layout.panelW(), panelY + layout.panelH());
        try {
            if (layout.verticalLayout()) {
                drawVerticalColumns(g, mc, data, keys, layout, panelX, panelY, displayValues, cc, contentScale, pad, hideBars, hideText);
            } else {
                drawHorizontalRows(g, mc, data, keys, layout, panelX, panelY, displayValues, cc, contentScale, pad, hideIcons, hideBars, hideText);
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
            int pad,
            boolean hideBars,
            boolean hideText
    ) {
        int columnGap = Math.max(2, (int) Math.round(HudDrawHelpers.VERTICAL_COLUMN_GAP * layout.scale()));
        // Two independent offsets, same split as the dynamic renderer: "Move Text and Icons" moves the
        // name labels, "Move Bars" moves the bars together with their percentage text.
        int textDx = layout.contentOffsetX();
        int textDy = layout.contentOffsetY();
        int barDx = MarieModuleSettings.barOffsetX(UiStatePersistence.get(), PANEL_ID);
        int barDy = MarieModuleSettings.barOffsetY(UiStatePersistence.get(), PANEL_ID);
        // Bar size scales the bar and the percentage text at its end; the text size doesn't touch that number.
        float barScale = ContentScaleController.resolveContentScale(MarieModuleSettings.barScale(UiStatePersistence.get(), PANEL_ID));
        int vBarW = Math.max(1, Math.round(layout.verticalBarW() * barScale));
        int vBarH = Math.max(1, Math.round(layout.verticalBarH() * barScale));
        int contentX = panelX + pad + layout.leftMargin();
        int pctH = (int) Math.ceil(9 * barScale);
        int barTop = panelY + pad + pctH + 2;
        int labelY = barTop + vBarH + 2;
        int pctY = panelY + pad;
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
            int barX = columnX + (layout.verticalColumnW() - vBarW) / 2;
            String label = HudDrawHelpers.nutrientLabel(key);
            int labelSw = (int) Math.ceil(mc.font.width(label) * contentScale);
            int labelX = columnX + (layout.verticalColumnW() - labelSw) / 2;
            int pct = Math.round(truePct * 100f);
            String pctText = pct + "%";
            int pctSw = (int) Math.ceil(mc.font.width(pctText) * barScale);
            int pctX = columnX + (layout.verticalColumnW() - pctSw) / 2;
            if (!hideBars) {
                HudDrawHelpers.drawScaledLabel(
                        g,
                        mc,
                        pctText,
                        pctX + barDx,
                        pctY + barDy,
                        HudDrawHelpers.pctColor(key, truePct),
                        barScale
                );
                HudDrawHelpers.drawRoundedVerticalBar(
                        g,
                        barX + barDx,
                        barTop + barDy,
                        vBarW,
                        vBarH,
                        displayPct,
                        HudDrawHelpers.barBackgroundColor(),
                        HudDrawHelpers.barFillColor(key, truePct)
                );
                float flash = MarieClientCache.flashAlpha(key);
                if (flash > 0f) {
                    int a = (int) (flash * 80);
                    int flashColor = (a << 24) | 0xFFFFFF;
                    g.fill(
                            barX + barDx,
                            barTop + barDy,
                            barX + barDx + vBarW,
                            barTop + barDy + vBarH,
                            flashColor
                    );
                }
            }
            if (!hideText) {
                HudDrawHelpers.drawScaledLabel(
                        g,
                        mc,
                        label,
                        labelX + textDx,
                        labelY + textDy,
                        HudDrawHelpers.labelColor(),
                        contentScale
                );
            }
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
            int pad,
            boolean hideIcons,
            boolean hideBars,
            boolean hideText
    ) {
        // Two independent offsets, same split as the dynamic renderer: "Move Text and Icons" moves the
        // icon and name label, "Move Bars" moves the bar together with its percentage text.
        int textDx = layout.contentOffsetX();
        int textDy = layout.contentOffsetY();
        int barDx = MarieModuleSettings.barOffsetX(UiStatePersistence.get(), PANEL_ID);
        int barDy = MarieModuleSettings.barOffsetY(UiStatePersistence.get(), PANEL_ID);
        int iconDx = MarieModuleSettings.iconOffsetX(UiStatePersistence.get(), PANEL_ID);
        int iconDy = MarieModuleSettings.iconOffsetY(UiStatePersistence.get(), PANEL_ID);
        // Bar size scales the bar and the percentage text at its end; the text size doesn't touch that number.
        float barScale = ContentScaleController.resolveContentScale(MarieModuleSettings.barScale(UiStatePersistence.get(), PANEL_ID));
        int barW = Math.max(1, Math.round(layout.barW() * barScale));
        int barH = Math.max(1, Math.round(HudDrawHelpers.BAR_H * barScale));
        int contentX = panelX + pad + layout.leftMargin();
        int y = panelY + pad;
        // Native item size (16px) times the user's persisted icon-size adjustment (independent of the
        // text size, and equal to it until first set) — same "box geometry (layout.iconSize(), rowH)
        // plays no part in it" split NutrientBarComponent uses for its own icon.
        float iconScale = ContentScaleController.resolveContentScale(MarieModuleSettings.iconScale(UiStatePersistence.get(), PANEL_ID));
        int iconSize = Math.round(16 * iconScale);
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
            int iconX = contentX + iconDx;
            int labelX = contentX + iconSize + HudDrawHelpers.ICON_LABEL_GAP;
            int barX = labelX + layout.maxLabelSw() + HudDrawHelpers.LABEL_BAR_GAP;
            if (!hideIcons) {
                HudDrawHelpers.renderIcon(g, key, iconX, rowCenterY - iconSize / 2 + iconDy, iconSize);
            }
            if (!hideText) {
                HudDrawHelpers.drawScaledLabel(g, mc, label, labelX + textDx, labelY + textDy, HudDrawHelpers.labelColor(), contentScale);
            }
            int barY = rowCenterY - barH / 2 + barDy;
            if (!hideBars) {
                HudDrawHelpers.drawRoundedBar(
                        g,
                        barX + barDx,
                        barY,
                        barW,
                        barH,
                        displayPct,
                        HudDrawHelpers.barBackgroundColor(),
                        HudDrawHelpers.barFillColor(key, truePct)
                );
                float flash = MarieClientCache.flashAlpha(key);
                if (flash > 0f) {
                    int a = (int) (flash * 80);
                    int flashColor = (a << 24) | 0xFFFFFF;
                    g.fill(barX + barDx, barY, barX + barDx + barW, barY + barH, flashColor);
                }
                int pct = Math.round(truePct * 100f);
                int pctX = barX + barDx + barW + HudDrawHelpers.BAR_PCT_GAP;
                int pctY = rowCenterY - (int) Math.ceil(9 * barScale) / 2 + barDy;
                HudDrawHelpers.drawScaledLabel(
                        g,
                        mc,
                        pct + "%",
                        pctX,
                        pctY,
                        HudDrawHelpers.pctColor(key, truePct),
                        barScale
                );
            }
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
