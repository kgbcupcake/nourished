package dev.maire.nourished.client.screen.diet.dynamic.options;

import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.network.chat.Component;

/**
 * The Diet Screen's tabbed options panel, built only through {@link MarieToolbox}. Every option is
 * a getter/setter over an existing {@link NourishedClientConfig} field: it writes in memory on each
 * change and persists with {@link NourishedClientConfig#saveNow} on commit, so the config file stays
 * the single source of truth.
 */
public final class DietOptionsPanel {

    private static final double PERCENT_STEP = 0.01d;

    private DietOptionsPanel() {}

    public static MarieComponent build() {
        Runnable save = NourishedClientConfig::saveNow;
        return MarieToolbox.panel(text("nourished.options.diet.title"))
                .tab(text("nourished.options.tab.appearance"))
                    .slider(text("nourished.options.diet.background_opacity"),
                            () -> cc().dietBackgroundOpacity(), v -> cc().setDietBackgroundOpacity(v), 0.0d, 1.0d, PERCENT_STEP, save)
                    .slider(text("nourished.options.text_brightness"),
                            () -> cc().dietTextBrightness(), v -> cc().setDietTextBrightness(v), 0.2d, 2.0d, PERCENT_STEP, save)
                    .slider(text("nourished.options.icon_brightness"),
                            () -> cc().dietIconBrightness(), v -> cc().setDietIconBrightness(v), 0.2d, 2.0d, PERCENT_STEP, save)
                    // Room left here for the in-game color picker (not built yet).
                .tab(text("nourished.options.tab.visibility"))
                    .toggle(text("nourished.options.diet.show_recent_meals"),
                            () -> cc().showRecentMeals(), v -> cc().setShowRecentMeals(v), save)
                    .toggle(text("nourished.options.diet.show_eat_more"),
                            () -> cc().showEatMoreOf(), v -> cc().setShowEatMoreOf(v), save)
                    // Only takes visible effect while FeatureFlagCache.enableTotalTracking() is on; bound as-is, not gated here.
                    .toggle(text("nourished.options.diet.show_calories_box"),
                            () -> cc().showCaloriesBox(), v -> cc().setShowCaloriesBox(v), save)
                    .toggle(text("nourished.options.diet.show_balance_box"),
                            () -> cc().showBalanceBox(), v -> cc().setShowBalanceBox(v), save)
                    // Read only when the inventory screen opens (ClientEvents#onScreenInit), so a change applies on the next open.
                    .toggle(text("nourished.options.diet.show_inventory_button"),
                            () -> cc().showDietScreenButton(), v -> cc().setShowDietScreenButton(v), save)
                .build();
    }

    private static NourishedClientConfig cc() {
        return NourishedClientConfig.get();
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
