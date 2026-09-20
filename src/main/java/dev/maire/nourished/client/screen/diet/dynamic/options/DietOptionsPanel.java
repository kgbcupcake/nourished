package dev.maire.nourished.client.screen.diet.dynamic.options;

import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.client.colors.NourishedColorSlots;
import dev.maire.nourished.client.colors.NourishedColors;
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
    /** The config default of {@code dietBackgroundOpacity} (204/255), which "Reset This Tab" restores. */
    private static final double DEFAULT_OPACITY = 204.0d / 255.0d;

    private DietOptionsPanel() {}

    public static MarieComponent build() {
        Runnable save = NourishedClientConfig::saveNow;
        MarieToolbox.PanelBuilder panel = MarieToolbox.panel(text("nourished.options.diet.title"))
                .tab(text("config.marieslib.moduleoptions.tab.appearance"))
                    .slider(text("config.marieslib.moduleoptions.backgroundOpacity"),
                            () -> cc().dietBackgroundOpacity(), v -> cc().setDietBackgroundOpacity(v), 0.0d, 1.0d, PERCENT_STEP, save)
                        .defaultValue(DEFAULT_OPACITY)
                    .slider(text("config.marieslib.moduleoptions.textBrightness"),
                            () -> cc().dietTextBrightness(), v -> cc().setDietTextBrightness(v), 0.2d, 2.0d, PERCENT_STEP, save)
                        .defaultValue(1.0d)
                    .slider(text("config.marieslib.moduleoptions.iconBrightness"),
                            () -> cc().dietIconBrightness(), v -> cc().setDietIconBrightness(v), 0.2d, 2.0d, PERCENT_STEP, save)
                        .defaultValue(1.0d)
                    .resetTab()
                .tab(text("nourished.options.tab.visibility"))
                    .toggle(text("nourished.options.diet.show_recent_meals"),
                            () -> cc().showRecentMeals(), v -> cc().setShowRecentMeals(v), save)
                        .defaultValue(true)
                    .toggle(text("nourished.options.diet.show_eat_more"),
                            () -> cc().showEatMoreOf(), v -> cc().setShowEatMoreOf(v), save)
                        .defaultValue(true)
                    // Only takes visible effect while FeatureFlagCache.enableTotalTracking() is on; bound as-is, not gated here.
                    .toggle(text("nourished.options.diet.show_calories_box"),
                            () -> cc().showCaloriesBox(), v -> cc().setShowCaloriesBox(v), save)
                        .defaultValue(true)
                    .toggle(text("nourished.options.diet.show_balance_box"),
                            () -> cc().showBalanceBox(), v -> cc().setShowBalanceBox(v), save)
                        .defaultValue(true)
                    // Read only when the inventory screen opens (ClientEvents#onScreenInit), so a change applies on the next open.
                    .toggle(text("nourished.options.diet.show_inventory_button"),
                            () -> cc().showDietScreenButton(), v -> cc().setShowDietScreenButton(v), save)
                        .defaultValue(true)
                    .resetTab();
        panel.colorTab(text("config.marieslib.moduleoptions.tab.colors"));
        NourishedColorSlots.addNutrients(panel);
        NourishedColorSlots.addFixed(panel, NourishedColors.DIET_PANEL, "nourished.options.color.panel");
        NourishedColorSlots.addFixed(panel, NourishedColors.DIET_TITLE, "nourished.options.color.title");
        NourishedColorSlots.addFixed(panel, NourishedColors.DIET_TODAY, "nourished.options.color.today_text");
        // Roles several Diet boxes draw live here once, so a shared key is never repeated across tabs. The
        // toggle's housing, border and lever colors (toggle.housing/border/lever) are deliberately left to colors.json.
        panel.colorTab(text("nourished.options.tab.shared"));
        NourishedColorSlots.addFixed(panel, NourishedColors.TEXT, "nourished.options.color.text");
        NourishedColorSlots.addFixed(panel, NourishedColors.TEXT_HEADER, "nourished.options.color.header_text");
        NourishedColorSlots.addFixed(panel, NourishedColors.TEXT_MUTED, "nourished.options.color.muted_text");
        NourishedColorSlots.addFixed(panel, NourishedColors.BORDER, "nourished.options.color.border");
        NourishedColorSlots.addFixed(panel, NourishedColors.DIVIDER, "nourished.options.color.divider");
        NourishedColorSlots.addFixed(panel, NourishedColors.DIET_BAR_TRACK, "nourished.options.color.bar_track");
        NourishedColorSlots.addFixed(panel, NourishedColors.TOGGLE_ON, "nourished.options.color.toggle_on");
        NourishedColorSlots.addFixed(panel, NourishedColors.TOGGLE_OFF, "nourished.options.color.toggle_off");
        return panel.build();
    }

    /**
     * The options panel for one Diet sub-box: Layout (Padding), Behavior (Move Text, Move Icons, Move All, Reset
     * Positions) and Appearance (Text size, Icon size, Text brightness, Icon brightness) over the sub-box's own
     * UI-state entries — the same panel the HUD boxes get from MariesLib; {@code hasBars} keeps the bar options for
     * the boxes that draw a bar (Calories, Balance) and drops them for the rest. Everything is kept in {@code DietScreenPersistence}; nothing goes into the config file.
     */
    public static MarieComponent forModule(String title, String moduleId, boolean hasBars) {
        return forModule(title, moduleId, hasBars, true, false);
    }

    /** Same, also choosing whether the box has icons to move and whether its header moves apart from its text (Active Effects: a title plus effect lines, no icons). */
    public static MarieComponent forModule(String title, String moduleId, boolean hasBars, boolean hasIcons, boolean hasHeader) {
        return forModule(title, moduleId, hasBars, hasIcons, hasHeader, null);
    }

    /** Same, with a Colors tab whose slots {@code colors} adds (null: no Colors tab). */
    public static MarieComponent forModule(String title, String moduleId, boolean hasBars, boolean hasIcons, boolean hasHeader,
                                           java.util.function.Consumer<MarieToolbox.PanelBuilder> colors) {
        MarieModuleSettings.StandardPanelBuilder panel = MarieModuleSettings.standardPanel(title, DietScreenPersistence.get(), moduleId)
                .storedBrightness();
        if (!hasBars) {
            panel.withoutBars();
        }
        if (!hasIcons) {
            panel.withoutIcons();
        }
        if (hasHeader) {
            panel.withHeader();
        }
        if (colors != null) {
            panel.extraTabs(p -> {
                p.colorTab(text("config.marieslib.moduleoptions.tab.colors"));
                colors.accept(p);
            });
        }
        return panel.build();
    }

    /** The Calories box's colors: the calorie value (its text and bar fill). */
    public static void caloriesColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.CALORIE_VALUE, "nourished.options.color.calorie");
    }

    /** The Balance box's colors: one per balance state. */
    public static void balanceColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_BALANCED, "nourished.options.color.balanced");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_LOW, "nourished.options.color.balance_low");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_EXCESS, "nourished.options.color.balance_excess");
    }

    /** The Active Effects box's colors: helpful and harmful effect lines. */
    public static void effectsColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.EFFECT_BENEFICIAL, "nourished.options.color.beneficial");
        NourishedColorSlots.addFixed(panel, NourishedColors.EFFECT_HARMFUL, "nourished.options.color.harmful");
    }

    private static NourishedClientConfig cc() {
        return NourishedClientConfig.get();
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
