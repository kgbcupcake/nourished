package dev.maire.nourished.client.screen.diet.classic;

import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Pure drawing primitives and color constants shared by {@link ClassicDietLeftPanel} and
 * {@link ClassicDietRightPanel} — extracted verbatim from the historical monolithic
 * {@code ClassicDietScreen}; no logic changes, only relocated.
 */
final class ClassicDietDrawHelpers {

    private ClassicDietDrawHelpers() {}

    // ── Colors ───────────────────────────────────────────────────────────────
    static final int COL_BG_RGB     = 0x00141414;  // outer background #141414
    static final int COL_PANEL_RGB  = 0x001E1E1E;  // inner panel #1E1E1E
    static final int COL_ROW_BG_RGB = 0x001E1E1E;  // matches inner card
    static final int COL_BORDER     = 0xFF3A3A3A;
    static final int COL_BORDER_LT  = 0xFF555555;
    static final int COL_SEG_EMPTY  = 0xFF2A2A2A;  // empty bar bg
    static final int COL_DIVIDER    = 0xFF2E2E2E;
    static final int COL_HEADER     = 0xFF888888;
    static final int COL_WHITE      = 0xFFFFFFFF;
    static final int COL_GRAY       = 0xFF666666;
    static final int COL_GOLD       = 0xFFFFD65C;
    static final int COL_CYAN       = 0xFF4DD9D9;
    static final int COL_GREEN      = 0xFF55FF55;
    static final int COL_ORANGE     = 0xFFFFAA00;
    static final int COL_RED        = 0xFFFF5555;
    static final int COL_PURPLE     = 0xFFA95FFF;
    static final int COL_LEGEND_TEXT = 0xFFE0E0E0; // base; legend uses dimLegend() ~12% darker
    /** Warm white/yellow (RGB) for nutrient bar flash overlay; alpha applied at draw time. */
    static final int COL_FLASH_RGB  = 0xFFFFE0;

    static void drawRoundedBox(GuiGraphics g, int x, int y, int w, int h) {
        int fill = panelColorWithOpacity(COL_ROW_BG_RGB, NourishedClientConfig.get().dietBackgroundOpacity());
        drawRoundedPanel(g, x, y, w, h, fill, COL_BORDER_LT, COL_BORDER);
    }

    static int panelColorWithOpacity(int rgb, double opacity) {
        int alpha = Mth.clamp((int) Math.round(opacity * 255.0d), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    static void drawRoundedPanel(GuiGraphics g, int x, int y, int w, int h, int fill, int borderLight, int borderDark) {
        int x2 = x + w;
        int y2 = y + h;
        g.fill(x + 2, y, x2 - 2, y2, fill);
        g.fill(x, y + 2, x2, y2 - 2, fill);
        // Strong rounded corners
        g.fill(x + 1, y + 1, x + 2, y + 2, fill);
        g.fill(x2 - 2, y + 1, x2 - 1, y + 2, fill);
        g.fill(x + 1, y2 - 2, x + 2, y2 - 1, fill);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, fill);
        // Borders with rounded corner cuts
        g.fill(x + 2, y, x2 - 2, y + 1, borderLight);
        g.fill(x + 2, y2 - 1, x2 - 2, y2, borderDark);
        g.fill(x, y + 2, x + 1, y2 - 2, borderLight);
        g.fill(x2 - 1, y + 2, x2, y2 - 2, borderDark);
        g.fill(x + 1, y + 1, x + 2, y + 2, borderLight);
        g.fill(x2 - 2, y + 1, x2 - 1, y + 2, borderLight);
        g.fill(x + 1, y2 - 2, x + 2, y2 - 1, borderDark);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, borderDark);
    }

    static void drawSolidBar(GuiGraphics g, int x, int y, int w, int h, float pct, int color) {
        int filled = (int)(w * Mth.clamp(pct, 0f, 1f));
        g.fill(x, y, x + w,      y + h, COL_SEG_EMPTY);
        g.fill(x, y, x + filled, y + h, color);
    }

    static void drawSegmentedBar(GuiGraphics g, int x, int y, int w, int h, float pct, int color) {
        int segs   = 10;
        int sw     = w / segs;
        int filled = Math.round(Mth.clamp(pct, 0f, 1f) * segs);
        for (int i = 0; i < segs; i++) {
            int sx = x + i * sw;
            g.fill(sx, y, sx + sw - 1, y + h, i < filled ? color : COL_SEG_EMPTY);
        }
    }

    /** ~12% darker legend text and swatches (brightness reduction). */
    static int dimLegend(int argb) {
        float f = 0.88f;
        int a = (argb >>> 24) & 0xFF;
        int r = Mth.clamp((int) (((argb >> 16) & 0xFF) * f), 0, 255);
        int g = Mth.clamp((int) (((argb >> 8) & 0xFF) * f), 0, 255);
        int b = Mth.clamp((int) ((argb & 0xFF) * f), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
