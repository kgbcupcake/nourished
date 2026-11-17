package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.api.MoveDrag;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;

/**
 * The Nutrient HUD's content drags inside its box: Move Text/Icons/Bars/All and Move Nutrient (one
 * row). The edit host only moves and resizes the box itself; it has no hook for this HUD's own text
 * offset, so these stay here until MariesLib gets one.
 */
final class HudContentMove {

    static final String CONTENT_OFFSET_ID = "nourished.hud.contentOffset";

    /** How much of the content's origin must stay inside the box; anything past the edge is clipped, not blocked. */
    private static final int MIN_VISIBLE_CONTENT = 14;

    private static int cachedOffsetX;
    private static int cachedOffsetY;
    private static boolean cacheLoaded;

    private final String panelId;
    private final MoveDrag moveDrag = new MoveDrag();
    // a plain saved translation, not its own box, so it never competes with the panel's drag
    private int offsetX;
    private int offsetY;

    private String rowDragKey;
    private double rowDragStartMouseX;
    private double rowDragStartMouseY;
    private int rowDragStartX;
    private int rowDragStartY;

    HudContentMove(String panelId) {
        this.panelId = panelId;
        UiStatePersistence.get().load(CONTENT_OFFSET_ID).ifPresent(state -> {
            offsetX = state.x();
            offsetY = state.y();
        });
    }

    int offsetX() {
        return offsetX;
    }

    int offsetY() {
        return offsetY;
    }

    /** "Reset Positions": puts the text offset back to zero (MariesLib resets the icon and bar offsets). */
    void reset() {
        offsetX = 0;
        offsetY = 0;
        persist();
    }

    private void persist() {
        UiStatePersistence.get().save(CONTENT_OFFSET_ID, new ComponentState(offsetX, offsetY, 0, 0, false, false, false, 0));
        cachedOffsetX = offsetX;
        cachedOffsetY = offsetY;
    }

    /** The saved text offset, for the in-game HUD, which has no instance of this class. */
    static int persistedOffsetX() {
        loadCache();
        return cachedOffsetX;
    }

    static int persistedOffsetY() {
        loadCache();
        return cachedOffsetY;
    }

    private static void loadCache() {
        if (cacheLoaded) {
            return;
        }
        cacheLoaded = true;
        UiStatePersistence.get().load(CONTENT_OFFSET_ID).ifPresent(state -> {
            cachedOffsetX = state.x();
            cachedOffsetY = state.y();
        });
    }

    private static int pad() {
        return Math.round(ContentScaleController.resolvePadding(HudDrawHelpers.PANEL_PAD * HudEditTarget.persistedPaddingScale()));
    }

    private static int clampX(int offset, Bounds panel, int leftMargin) {
        int min = -(pad() + leftMargin);
        int max = Math.max(min, panel.width() - pad() - leftMargin - MIN_VISIBLE_CONTENT);
        return Math.min(max, Math.max(min, offset));
    }

    private static int clampY(int offset, Bounds panel) {
        int min = -pad();
        int max = Math.max(min, panel.height() - pad() - MIN_VISIBLE_CONTENT);
        return Math.min(max, Math.max(min, offset));
    }

    /** Whether a click in the box should move content rather than the box. */
    boolean contentModeOn() {
        return HudRowMove.activeKey() != null || MarieModuleSettings.activeMoveMode(UiStatePersistence.get(), panelId) != null;
    }

    /** Starts a content drag if a move mode is on and the click is inside {@code bounds}. */
    boolean click(double mouseX, double mouseY, Bounds bounds) {
        if (!bounds.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        // Move Nutrient wins over the panel-wide modes
        String rowKey = HudRowMove.activeKey();
        if (rowKey != null) {
            rowDragKey = rowKey;
            rowDragStartMouseX = mouseX;
            rowDragStartMouseY = mouseY;
            rowDragStartX = HudRowMove.offsetX(rowKey);
            rowDragStartY = HudRowMove.offsetY(rowKey);
            return true;
        }
        var store = UiStatePersistence.get();
        MoveDrag.Mode mode = MarieModuleSettings.activeMoveMode(store, panelId);
        if (mode == null) {
            return false;
        }
        switch (mode) {
            case ICONS -> moveDrag.start(mode, mouseX, mouseY, MarieModuleSettings.iconOffsetX(store, panelId), MarieModuleSettings.iconOffsetY(store, panelId));
            case BARS -> moveDrag.start(mode, mouseX, mouseY, MarieModuleSettings.barOffsetX(store, panelId), MarieModuleSettings.barOffsetY(store, panelId));
            case ALL -> moveDrag.startAll(mouseX, mouseY, offsetX, offsetY,
                    MarieModuleSettings.iconOffsetX(store, panelId), MarieModuleSettings.iconOffsetY(store, panelId),
                    MarieModuleSettings.barOffsetX(store, panelId), MarieModuleSettings.barOffsetY(store, panelId));
            case TEXT -> moveDrag.start(mode, mouseX, mouseY, offsetX, offsetY);
            // this HUD has no inner icon or header to move
            default -> { }
        }
        return true;
    }

    /** Follows the mouse while a content drag is active; {@code bounds} may be null when there's no box. */
    boolean drag(double mouseX, double mouseY, Bounds bounds, int leftMargin) {
        if (rowDragKey != null) {
            HudRowMove.setOffset(rowDragKey,
                    rowDragStartX + (int) Math.round(mouseX - rowDragStartMouseX),
                    rowDragStartY + (int) Math.round(mouseY - rowDragStartMouseY), bounds);
            return true;
        }
        if (!moveDrag.isActive()) {
            return false;
        }
        if (bounds == null) {
            return true;
        }
        var store = UiStatePersistence.get();
        int x = clampX(moveDrag.offsetX(mouseX), bounds, leftMargin);
        int y = clampY(moveDrag.offsetY(mouseY), bounds);
        switch (moveDrag.mode()) {
            case ICONS -> MarieModuleSettings.setIconOffset(store, panelId, x, y);
            case BARS -> MarieModuleSettings.setBarOffset(store, panelId, x, y);
            case ALL -> {
                // one drag shifts all three offsets by the same amount from where each started
                int dx = moveDrag.offsetX(mouseX);
                int dy = moveDrag.offsetY(mouseY);
                offsetX = clampX(moveDrag.baseX(MoveDrag.Mode.TEXT) + dx, bounds, leftMargin);
                offsetY = clampY(moveDrag.baseY(MoveDrag.Mode.TEXT) + dy, bounds);
                MarieModuleSettings.setIconOffset(store, panelId, clampX(moveDrag.baseX(MoveDrag.Mode.ICONS) + dx, bounds, leftMargin), clampY(moveDrag.baseY(MoveDrag.Mode.ICONS) + dy, bounds));
                MarieModuleSettings.setBarOffset(store, panelId, clampX(moveDrag.baseX(MoveDrag.Mode.BARS) + dx, bounds, leftMargin), clampY(moveDrag.baseY(MoveDrag.Mode.BARS) + dy, bounds));
            }
            default -> {
                offsetX = x;
                offsetY = y;
            }
        }
        return true;
    }

    /** Saves whatever the finished drag changed. */
    boolean release() {
        if (rowDragKey != null) {
            HudRowMove.commit(rowDragKey);
            rowDragKey = null;
            return true;
        }
        if (!moveDrag.isActive()) {
            return false;
        }
        MoveDrag.Mode mode = moveDrag.mode();
        moveDrag.stop();
        var store = UiStatePersistence.get();
        switch (mode) {
            case ICONS -> MarieModuleSettings.commitIconOffset(store, panelId);
            case BARS -> MarieModuleSettings.commitBarOffset(store, panelId);
            case ALL -> {
                persist();
                MarieModuleSettings.commitIconOffset(store, panelId);
                MarieModuleSettings.commitBarOffset(store, panelId);
            }
            default -> persist();
        }
        return true;
    }
}
