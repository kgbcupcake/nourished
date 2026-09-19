package dev.maire.nourished.client.hud.dynamic.options;

import dev.marie.framework.ui.api.MarieToolbox.PanelBuilder;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.maire.nourished.client.UiStatePersistence;
import net.minecraft.network.chat.Component;

/**
 * The two UI-state options every HUD module box carries — Text Scale and Padding — as toolbox rows over the module's own persisted {@link ComponentState}, exactly the
 * store {@code ScaleConfigPanel}'s built-in rows use. UI-state setters save through
 * {@link UiStatePersistence} immediately, so there is nothing left to do on commit.
 */
final class LayoutOptionRows {

    /** Same suffix {@code ScaleConfigPanel} keys its "Move Text and Icons" flag with — a copy of that class's private {@code moveContentKey}, only used to switch a stale flag off. */
    private static final String MOVE_CONTENT_SUFFIX = "#moveContent";

    private static final ComponentState BLANK = new ComponentState(0, 0, 0, 0, false, false, false, 0);
    private static final Runnable ALREADY_SAVED = () -> {};

    /** Same step {@code ScaleConfigPanel} uses per scroll notch for its scale sliders. */
    private static final double SCALE_STEP = 0.05d;

    private LayoutOptionRows() {}

    /**
     * Appends Text Scale and Padding for {@code panelId} to the current tab. The old "Move Text and
     * Icons" option is intentionally gone from every module box, so this also switches off a flag a
     * previous session may have left on — with no toggle to clear it, content-move mode would
     * otherwise stay stuck on and hijack dragging the box.
     */
    static PanelBuilder addTo(PanelBuilder tab, String panelId) {
        clearMoveContent(panelId);
        return tab
                .slider(text("config.marieslib.scaleconfig.textScale"),
                        () -> state(panelId).contentScale(),
                        v -> UiStatePersistence.get().save(panelId, withContentScale(state(panelId), v)),
                        ContentScaleController.SCALE_STORAGE_MIN, ContentScaleController.SCALE_STORAGE_MAX, SCALE_STEP,
                        ALREADY_SAVED)
                .slider(text("config.marieslib.scaleconfig.padding"),
                        () -> state(panelId).paddingScale(),
                        v -> UiStatePersistence.get().save(panelId, withPaddingScale(state(panelId), v)),
                        ContentScaleController.SCALE_STORAGE_MIN, ContentScaleController.SCALE_STORAGE_MAX, SCALE_STEP,
                        ALREADY_SAVED);
    }

    private static void clearMoveContent(String panelId) {
        if (moveContent(panelId)) {
            UiStatePersistence.get().save(panelId + MOVE_CONTENT_SUFFIX, new ComponentState(0, 0, 0, 0, false, false, false, 0));
        }
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static ComponentState state(String panelId) {
        return UiStatePersistence.get().load(panelId).orElse(BLANK);
    }

    private static boolean moveContent(String panelId) {
        return UiStatePersistence.get().load(panelId + MOVE_CONTENT_SUFFIX).map(ComponentState::collapsed).orElse(false);
    }

    // Read fresh at write time and touch only one field, exactly as ScaleConfigPanel does, so the
    // panel's position/size and the other scale are never clobbered.
    private static ComponentState withContentScale(ComponentState base, double contentScale) {
        return new ComponentState(base.x(), base.y(), base.width(), base.height(), base.collapsed(),
                base.widthManual(), base.heightManual(), base.leftMargin(), contentScale, base.paddingScale());
    }

    private static ComponentState withPaddingScale(ComponentState base, double paddingScale) {
        return new ComponentState(base.x(), base.y(), base.width(), base.height(), base.collapsed(),
                base.widthManual(), base.heightManual(), base.leftMargin(), base.contentScale(), paddingScale);
    }
}
