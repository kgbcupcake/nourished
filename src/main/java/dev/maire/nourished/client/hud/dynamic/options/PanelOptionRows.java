package dev.maire.nourished.client.hud.dynamic.options;

import dev.marie.framework.ui.api.MarieToolbox.PanelBuilder;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.maire.nourished.client.UiStatePersistence;
import net.minecraft.network.chat.Component;

/**
 * Toolbox rows every HUD module box shares, over the module's own UI-state store ({@link
 * UiStatePersistence}, the same store {@code ScaleConfigPanel}'s built-in rows use): Padding, Move Text and
 * Icons, and the independent Text size / Icon size pair (see {@link PanelScales}). UI-state setters save through the
 * persistence provider immediately, so there is nothing left to do on commit.
 */
final class PanelOptionRows {

    /** Same suffix {@code ScaleConfigPanel} keys its "Move Text and Icons" flag with — a copy of that class's private {@code moveContentKey}/{@code toggleMoveContent}, since MariesLib exposes only the getter ({@code isMoveContentEnabled}), no setter. Follow-up: expose a public accessor there and delete this copy. */
    private static final String MOVE_CONTENT_SUFFIX = "#moveContent";

    private static final ComponentState BLANK = new ComponentState(0, 0, 0, 0, false, false, false, 0);
    private static final Runnable ALREADY_SAVED = () -> {};

    /** Same step {@code ScaleConfigPanel} uses per scroll notch for its scale sliders. */
    private static final double SCALE_STEP = 0.05d;

    private PanelOptionRows() {}

    /** Appends Padding for {@code panelId} to the current tab. */
    static PanelBuilder addPadding(PanelBuilder tab, String panelId) {
        return tab.slider(text("config.marieslib.scaleconfig.padding"),
                () -> state(panelId).paddingScale(),
                v -> UiStatePersistence.get().save(panelId, withPaddingScale(state(panelId), v)),
                ContentScaleController.SCALE_STORAGE_MIN, ContentScaleController.SCALE_STORAGE_MAX, SCALE_STEP,
                ALREADY_SAVED);
    }

    /** Appends the independent Text size and Icon size sliders for {@code panelId} to the current tab. */
    static PanelBuilder addSizes(PanelBuilder tab, String panelId) {
        return tab
                .slider(text("nourished.options.text_size"),
                        () -> PanelScales.textScale(panelId), v -> PanelScales.setTextScale(panelId, v),
                        ContentScaleController.SCALE_STORAGE_MIN, ContentScaleController.SCALE_STORAGE_MAX, SCALE_STEP,
                        ALREADY_SAVED)
                .slider(text("nourished.options.icon_size"),
                        () -> PanelScales.iconScale(panelId), v -> PanelScales.setIconScale(panelId, v),
                        ContentScaleController.SCALE_STORAGE_MIN, ContentScaleController.SCALE_STORAGE_MAX, SCALE_STEP,
                        ALREADY_SAVED);
    }

    /**
     * Appends the "Move Text and Icons" toggle for {@code panelId} to the current tab — the same
     * {@code #moveContent} UI-state flag {@code ScaleConfigPanel}'s built-in row uses (and its
     * {@code isMoveContentEnabled} getter reads back), so a host box polling that getter keeps working.
     */
    static PanelBuilder addMoveContent(PanelBuilder tab, String panelId) {
        return tab.toggle(text("config.marieslib.scaleconfig.moveContent"),
                () -> UiStatePersistence.get().load(panelId + MOVE_CONTENT_SUFFIX).map(ComponentState::collapsed).orElse(false),
                v -> UiStatePersistence.get().save(panelId + MOVE_CONTENT_SUFFIX, new ComponentState(0, 0, 0, 0, v, false, false, 0)),
                ALREADY_SAVED);
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static ComponentState state(String panelId) {
        return UiStatePersistence.get().load(panelId).orElse(BLANK);
    }

    private static ComponentState withPaddingScale(ComponentState base, double paddingScale) {
        return new ComponentState(base.x(), base.y(), base.width(), base.height(), base.collapsed(),
                base.widthManual(), base.heightManual(), base.leftMargin(), base.contentScale(), paddingScale);
    }
}
