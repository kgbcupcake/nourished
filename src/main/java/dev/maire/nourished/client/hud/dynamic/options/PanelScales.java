package dev.maire.nourished.client.hud.dynamic.options;

import dev.marie.framework.ui.component.ComponentState;
import dev.maire.nourished.client.UiStatePersistence;

/**
 * A HUD module's two independent size multipliers, both kept in the same UI-state store as the rest
 * of the module's scale settings. Text size is the module's own {@link ComponentState#contentScale()};
 * icon size lives under a sibling {@code <panelId>#iconScale} key (its {@code contentScale} field,
 * the same field-reuse {@code ScaleConfigPanel} does for {@code #moveContent}). While that key has
 * never been written, icons follow the text size exactly as they did before the two were split, so
 * an existing profile looks unchanged; the first edit of the text size pins the icon size at its
 * current value first, so from then on the two move independently.
 */
public final class PanelScales {

    private static final String ICON_SCALE_SUFFIX = "#iconScale";
    private static final ComponentState BLANK = new ComponentState(0, 0, 0, 0, false, false, false, 0);

    private PanelScales() {}

    /** Text size multiplier for {@code panelId}. */
    public static double textScale(String panelId) {
        return UiStatePersistence.get().load(panelId).orElse(BLANK).contentScale();
    }

    /** Icon size multiplier for {@code panelId}; the text size until an icon size has been set. */
    public static double iconScale(String panelId) {
        return UiStatePersistence.get().load(panelId + ICON_SCALE_SUFFIX)
                .map(ComponentState::contentScale)
                .orElseGet(() -> textScale(panelId));
    }

    static void setTextScale(String panelId, double value) {
        if (UiStatePersistence.get().load(panelId + ICON_SCALE_SUFFIX).isEmpty()) {
            setIconScale(panelId, textScale(panelId));
        }
        // Read fresh and touch only one field so the panel's position/size/padding are never clobbered.
        ComponentState base = UiStatePersistence.get().load(panelId).orElse(BLANK);
        UiStatePersistence.get().save(panelId, new ComponentState(base.x(), base.y(), base.width(), base.height(),
                base.collapsed(), base.widthManual(), base.heightManual(), base.leftMargin(), value, base.paddingScale()));
    }

    static void setIconScale(String panelId, double value) {
        UiStatePersistence.get().save(panelId + ICON_SCALE_SUFFIX,
                new ComponentState(0, 0, 0, 0, false, false, false, 0, value, ComponentState.DEFAULT_PADDING_SCALE));
    }
}
