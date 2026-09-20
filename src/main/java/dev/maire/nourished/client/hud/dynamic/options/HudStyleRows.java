package dev.maire.nourished.client.hud.dynamic.options;

import dev.marie.framework.ui.api.MarieToolbox;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.network.chat.Component;

/**
 * The border and shade rows of the Calorie History and Activity Log boxes' Style tab, over their existing
 * {@link NourishedClientConfig} fields; added through {@code StandardPanelBuilder#styleRows} so they sit above
 * the tab's "Reset This Tab" button.
 */
public final class HudStyleRows {

    private static final double PERCENT_STEP = 0.01d;

    private HudStyleRows() {}

    public static void calorieHistory(MarieToolbox.PanelBuilder panel) {
        Runnable save = NourishedClientConfig::saveNow;
        panel.slider(text("nourished.options.hud.border_opacity"),
                        () -> cc().calorieHudBorderOpacity(), v -> cc().setCalorieHudBorderOpacity(v), 0.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(1.0d)
                .slider(text("nourished.options.hud.background_shade"),
                        () -> cc().calorieHudBackgroundShade(), v -> cc().setCalorieHudBackgroundShade(v), -1.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(0.0d)
                .slider(text("nourished.options.hud.border_shade"),
                        () -> cc().calorieHudBorderShade(), v -> cc().setCalorieHudBorderShade(v), -1.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(0.0d);
    }

    public static void nutrientHud(MarieToolbox.PanelBuilder panel) {
        Runnable save = NourishedClientConfig::saveNow;
        panel.slider(text("nourished.options.hud.border_opacity"),
                        () -> cc().hudBorderOpacity(), v -> cc().setHudBorderOpacity(v), 0.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(1.0d)
                .slider(text("nourished.options.hud.background_shade"),
                        () -> cc().hudBackgroundShade(), v -> cc().setHudBackgroundShade(v), -1.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(0.0d)
                .slider(text("nourished.options.hud.border_shade"),
                        () -> cc().hudBorderShade(), v -> cc().setHudBorderShade(v), -1.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(0.0d);
    }

    public static void activityLog(MarieToolbox.PanelBuilder panel) {
        Runnable save = NourishedClientConfig::saveNow;
        panel.slider(text("nourished.options.hud.border_opacity"),
                        () -> cc().activityLogHudBorderOpacity(), v -> cc().setActivityLogHudBorderOpacity(v), 0.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(1.0d)
                .slider(text("nourished.options.hud.background_shade"),
                        () -> cc().activityLogHudBackgroundShade(), v -> cc().setActivityLogHudBackgroundShade(v), -1.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(0.0d)
                .slider(text("nourished.options.hud.border_shade"),
                        () -> cc().activityLogHudBorderShade(), v -> cc().setActivityLogHudBorderShade(v), -1.0d, 1.0d, PERCENT_STEP, save)
                .defaultValue(0.0d);
    }

    private static NourishedClientConfig cc() {
        return NourishedClientConfig.get();
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
