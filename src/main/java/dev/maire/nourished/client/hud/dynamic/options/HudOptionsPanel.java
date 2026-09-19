package dev.maire.nourished.client.hud.dynamic.options;

import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.network.chat.Component;

/**
 * The Nutrient HUD's tabbed options panel, built only through {@link MarieToolbox}. Every option is
 * a getter/setter over a value that already exists — a {@link NourishedClientConfig} field, or the
 * HUD panel's own UI-state record via MariesLib's shared module rows — so nothing is stored here and
 * the config file keeps working. Config-backed options write in memory on every change and persist
 * with {@link NourishedClientConfig#saveNow}.
 */
public final class HudOptionsPanel {

    private static final double PERCENT_STEP = 0.01d;

    private HudOptionsPanel() {}

    /**
     * @param panelId         the HUD panel's persisted UI-state key (the one its scale/padding/position live under)
     * @param resetTextOffset puts the panel's own text offset back to zero for "Reset Positions"
     */
    public static MarieComponent build(String panelId, Runnable resetTextOffset) {
        Runnable save = NourishedClientConfig::saveNow;
        var ui = UiStatePersistence.get();
        return MarieToolbox.panel(text("nourished.hud.nutrientPanel.label"))
                .tab(text("config.marieslib.moduleoptions.tab.layout"))
                    .padding(ui, panelId)
                    .toggle(text("nourished.options.hud.vertical_layout"),
                            () -> cc().hudVerticalLayout(), v -> cc().setHudVerticalLayout(v), save)
                .tab(text("config.marieslib.moduleoptions.tab.behavior"))
                    .toggle(text("nourished.options.hud.reveal_on_gain"),
                            () -> cc().hudRevealOnNutrientGain(), v -> cc().setHudRevealOnNutrientGain(v), save)
                    .slider(text("nourished.options.hud.hide_above"),
                            () -> cc().hudHideAboveThreshold(), v -> cc().setHudHideAboveThreshold(v), 0.0d, 1.0d, PERCENT_STEP, save)
                    .slider(text("nourished.options.hud.show_above"),
                            () -> cc().hudShowAboveThreshold(), v -> cc().setHudShowAboveThreshold(v), 0.0d, 1.0d, PERCENT_STEP, save)
                        // "Show above" only re-reveals bars the hide rule hid, so it does nothing while hide is off (1.0).
                        .enabledWhen(() -> cc().hudHideAboveThreshold() < 1.0d)
                    .moveToggles(ui, panelId)
                    .resetPositions(ui, panelId, resetTextOffset)
                .tab(text("config.marieslib.moduleoptions.tab.appearance"))
                    .textAndIconSizes(ui, panelId)
                    .barSize(ui, panelId)
                    .slider(text("config.marieslib.moduleoptions.textBrightness"),
                            () -> cc().hudTextBrightness(), v -> cc().setHudTextBrightness(v), 0.2d, 2.0d, PERCENT_STEP, save)
                    .slider(text("config.marieslib.moduleoptions.iconBrightness"),
                            () -> cc().hudIconBrightness(), v -> cc().setHudIconBrightness(v), 0.2d, 2.0d, PERCENT_STEP, save)
                    .slider(text("config.marieslib.moduleoptions.backgroundOpacity"),
                            () -> cc().hudBackgroundOpacity(), v -> cc().setHudBackgroundOpacity(v), 0.0d, 1.0d, PERCENT_STEP, save)
                    // Room left here for the in-game color picker (not built yet).
                .build();
    }

    private static NourishedClientConfig cc() {
        return NourishedClientConfig.get();
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
