package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.client.hud.dynamic.layout.HudLayout;

import java.util.List;

/**
 * Row scrolling for the Nutrient HUD when its box is too short for every nutrient. The mouse wheel
 * moves it in edit mode, and the in-game HUD honors the same offset.
 */
public final class HudRowScroll {

    private static int scrollOffset;
    // what the last scrolledKeys call saw, for the indicator
    private static int lastTotalRows;
    private static int lastCapacity;

    private HudRowScroll() {}

    /** How many rows fit in the box (at least one); a row counts once half of it is visible, since the panel clip cuts the rest. */
    private static int rowCapacity(HudLayout.Layout layout) {
        int pad = Math.round(ContentScaleController.resolvePadding(HudDrawHelpers.PANEL_PAD * HudEditTarget.persistedPaddingScale()));
        int availableH = layout.panelH() - 2 * pad;
        return Math.max(1, (availableH + HudDrawHelpers.ROW_GAP + layout.rowH() / 2) / (layout.rowH() + HudDrawHelpers.ROW_GAP));
    }

    private static int maxScroll(int rowCount, HudLayout.Layout layout) {
        return layout.verticalLayout() ? 0 : Math.max(0, rowCount - rowCapacity(layout));
    }

    /** Moves one row per wheel notch; false when there's nothing to scroll. */
    static boolean scroll(int rowCount, HudLayout.Layout layout, double scrollY) {
        int max = maxScroll(rowCount, layout);
        if (max <= 0) {
            return false;
        }
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    /** The rows that fit in {@code layout}'s box from the scroll offset on — all of them when they fit, and always in the vertical layout. */
    public static List<String> scrolledKeys(List<String> keys, HudLayout.Layout layout) {
        int max = maxScroll(keys.size(), layout);
        scrollOffset = Math.min(scrollOffset, max);
        lastTotalRows = keys.size();
        lastCapacity = max == 0 ? keys.size() : rowCapacity(layout);
        if (max == 0) {
            return keys;
        }
        return keys.subList(scrollOffset, Math.min(keys.size(), scrollOffset + lastCapacity));
    }

    /** Thin track and thumb on the box's right inner edge, only while some rows are scrolled out of view. */
    public static void drawScrollIndicator(RenderContext context, Bounds bounds) {
        if (lastTotalRows <= lastCapacity) {
            return;
        }
        int trackH = Math.max(1, bounds.height() - 4);
        int thumbH = Math.max(4, trackH * lastCapacity / lastTotalRows);
        int max = lastTotalRows - lastCapacity;
        int thumbY = bounds.y() + 2 + (trackH - thumbH) * scrollOffset / max;
        int trackX = bounds.x() + bounds.width() - 3;
        context.fillRect(trackX, bounds.y() + 2, 2, trackH, context.theme().color(ThemeKey.BAR_BACKGROUND));
        context.fillRect(trackX, thumbY, 2, thumbH, MarieColors.resolveColor(NourishedColors.EDIT_OUTLINE));
    }
}
