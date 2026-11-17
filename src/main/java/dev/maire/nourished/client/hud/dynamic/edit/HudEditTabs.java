package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;

import java.util.List;

/**
 * Collapsed-tab chrome for the Nutrient HUD's edit mode: the box starts as a small tab at the top-left
 * so a fresh or reset box never lands on top of anything, and a click opens it. {@link #open} also
 * collapses any other panel listed in {@link #ORDER}, should another HUD box ever join it.
 */
public final class HudEditTabs {

    private static final int MARGIN = 8;
    private static final int GAP = 6;
    static final int TAB_HEIGHT = 18;
    static final int TAB_WIDTH = 150;
    private static final int COLLAPSE_BUTTON_SIZE = 9;

    /** Fixed top-to-bottom order of the left-side tab stack. */
    private static final List<String> ORDER = List.of("nourished.hud.panel");

    private static final String SUFFIX = "#editTabCollapsed";

    private HudEditTabs() {}

    /** Collapsed until the player has clicked this panel's tab open at least once — same "starts collapsed" default a fresh {@code ScaleConfigPanel} entry has. */
    public static boolean isCollapsed(PersistenceProvider persistence, String panelId) {
        return persistence.load(panelId + SUFFIX).map(ComponentState::collapsed).orElse(true);
    }

    public static void setCollapsed(PersistenceProvider persistence, String panelId, boolean collapsed) {
        persistence.save(panelId + SUFFIX, new ComponentState(0, 0, 0, 0, collapsed, false, false, 0));
    }

    /**
     * Expands {@code panelId}'s box and collapses every other panel in {@link #ORDER} — the tab-click
     * entry point every panel's {@code mouseClicked} should call instead of {@link #setCollapsed}
     * directly, so expanding one always leaves at most one box open across the group.
     */
    public static void open(PersistenceProvider persistence, String panelId) {
        for (String other : ORDER) {
            if (!other.equals(panelId)) {
                setCollapsed(persistence, other, true);
            }
        }
        setCollapsed(persistence, panelId, false);
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
