package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;

import java.util.List;

/**
 * Collapsed-tab chrome for the Activity Log and Calorie History HUD boxes' own edit mode — mirrors
 * {@code ScaleConfigPanel}'s "tabs first, click one to open its window" pattern (see the Nutrient
 * HUD/Calorie History settings tabs stacked at the top-right), but stacked at the top-left instead,
 * for these two panels' actual draggable/resizable boxes rather than their settings windows. Both
 * start collapsed so a freshly reset or first-installed box is never dropped straight on top of its
 * sibling (see {@code ActivityLogHudPanel}/{@code CalorieHudScreen}).
 */
public final class HudEditTabs {

    private static final int MARGIN = 8;
    private static final int GAP = 6;
    static final int TAB_HEIGHT = 18;
    static final int TAB_WIDTH = 150;
    private static final int COLLAPSE_BUTTON_SIZE = 9;

    /**
     * Fixed top-to-bottom order for the left-side tab stack, independent of which panel's {@code
     * render()} happens to run first in a given frame (unlike {@code AnchorStack}'s claim-order
     * approach, not worth reusing for just these two known panels).
     */
    private static final List<String> ORDER = List.of(
            "nourished.activityLogHud.panel",
            "nourished.calorieHud.panel"
    );

    private static final String SUFFIX = "#editTabCollapsed";

    private HudEditTabs() {}

    /** Collapsed until the player has clicked this panel's tab open at least once — same "starts collapsed" default a fresh {@code ScaleConfigPanel} entry has. */
    public static boolean isCollapsed(PersistenceProvider persistence, String panelId) {
        return persistence.load(panelId + SUFFIX).map(ComponentState::collapsed).orElse(true);
    }

    public static void setCollapsed(PersistenceProvider persistence, String panelId, boolean collapsed) {
        persistence.save(panelId + SUFFIX, new ComponentState(0, 0, 0, 0, collapsed, false, false, 0));
    }

    /** This panel's fixed slot in the left-side tab stack. */
    public static Bounds tabBounds(String panelId) {
        int index = Math.max(0, ORDER.indexOf(panelId));
        int y = MARGIN + index * (TAB_HEIGHT + GAP);
        return new Bounds(MARGIN, y, TAB_WIDTH, TAB_HEIGHT);
    }

    public static void drawTab(RenderContext context, String label, int accent, Bounds bounds) {
        int panelBg = context.theme().color(ThemeKey.PANEL_BACKGROUND);
        int border = context.theme().color(ThemeKey.BORDER);
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, panelBg, border);
        context.fillRect(bounds.x() + 1, bounds.y() + 1, 2, bounds.height() - 2, accent);
        context.drawText(label, bounds.x() + 6, bounds.y() + (bounds.height() - 8) / 2, accent, 0.85f);
    }

    /** Small "x" toggle drawn at a box's top-right corner while it's expanded, to collapse it back to a tab. */
    public static Bounds collapseButtonBounds(Bounds boxBounds) {
        return new Bounds(boxBounds.x() + boxBounds.width() - 2 - COLLAPSE_BUTTON_SIZE, boxBounds.y() + 2, COLLAPSE_BUTTON_SIZE, COLLAPSE_BUTTON_SIZE);
    }

    public static void drawCollapseButton(RenderContext context, Bounds bounds, int accent) {
        int bg = (0x40 << 24) | (accent & 0x00FFFFFF);
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, bg, accent);
        context.drawText("x", bounds.x() + 2, bounds.y() + 1, accent, 0.65f);
    }
}
