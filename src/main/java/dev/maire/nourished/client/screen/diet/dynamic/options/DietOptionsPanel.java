package dev.maire.nourished.client.screen.diet.dynamic.options;

import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.api.StandardPanelBuilder;
import dev.maire.nourished.client.screen.diet.dynamic.edit.DietScreenEditTarget;
import dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.client.colors.NourishedColorSlots;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.network.chat.Component;

/**
 * The Diet Screen's tabbed options panel, built only through {@link MarieToolbox}, tab order and Style groups matching {@link MarieModuleSettings#standardPanel}. Every option is
 * a getter/setter over an existing {@link NourishedClientConfig} field: it writes in memory on each
 * change and persists with {@link NourishedClientConfig#saveNow} on commit, so the config file stays
 * the single source of truth.
 */
public final class DietOptionsPanel {

    private static final double PERCENT_STEP = 0.01d;
    private static final double SCALE_STEP = 0.05d;
    /** The config default of {@code dietBackgroundOpacity} (204/255), which "Reset This Tab" restores. */
    private static final double DEFAULT_OPACITY = 204.0d / 255.0d;

    private DietOptionsPanel() {}

    public static MarieComponent build() {
        Runnable save = NourishedClientConfig::saveNow;
        return MarieModuleSettings.standardPanel(text("nourished.options.diet.title"), DietScreenPersistence.get(), DietScreenEditTarget.PANEL_ID)
                // The whole screen has no text, icons or padding of its own to size or move; those live on its five boxes.
                .withoutPadding()
                .withoutMoveAndHide()
                .withoutSizes()
                .opacity(() -> cc().dietBackgroundOpacity(), v -> cc().setDietBackgroundOpacity(v), DEFAULT_OPACITY)
                .textBrightness(() -> cc().dietTextBrightness(), v -> cc().setDietTextBrightness(v))
                .iconBrightness(() -> cc().dietIconBrightness(), v -> cc().setDietIconBrightness(v))
                .onCommit(save)
                .layoutRows(p -> p
                        // Sizes of the panel and its two sub-boxes as laid out; separate from each box's own Text size.
                        .slider(text("nourished.options.diet.panel_size"),
                                () -> cc().dietScale(), v -> cc().setDietScale(v), 0.5d, 1.5d, SCALE_STEP, save)
                            .defaultValue(1.0d)
                        .slider(text("nourished.options.diet.recent_meals_box_size"),
                                () -> cc().recentMealsBoxScale(), v -> cc().setRecentMealsBoxScale(v), 0.5d, 1.5d, SCALE_STEP, save)
                            .defaultValue(1.0d)
                        .slider(text("nourished.options.diet.eat_more_box_size"),
                                () -> cc().eatMoreBoxScale(), v -> cc().setEatMoreBoxScale(v), 0.5d, 1.5d, SCALE_STEP, save)
                            .defaultValue(1.0d))
                .behaviorRows(p -> p
                        .toggle(text("nourished.options.diet.drag_bars"),
                                () -> cc().dietBarDragEnabled(), v -> cc().setDietBarDragEnabled(v), save)
                            .defaultValue(true)
                        .button(text("nourished.options.diet.reset_bar_order"), text("nourished.options.diet.reset_caption"),
                                () -> cc().resetDietBarOrder(), save)
                        .section(text("nourished.options.hud.section.visibility"))
                        .toggle(text("nourished.options.diet.show_recent_meals"),
                                () -> cc().showRecentMeals(), v -> cc().setShowRecentMeals(v), save)
                            .defaultValue(true)
                        .toggle(text("nourished.options.diet.show_eat_more"),
                                () -> cc().showEatMoreOf(), v -> cc().setShowEatMoreOf(v), save)
                            .defaultValue(true)
                        .toggle(text("nourished.options.diet.show_active_effects"),
                                () -> cc().showActiveEffects(), v -> cc().setShowActiveEffects(v), save)
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
                        .endSection()
                        .resetTab())
                .extraTabs(panel -> {
                    panel.colorTab(text("config.marieslib.moduleoptions.tab.colors"));
                    NourishedColorSlots.addNutrients(panel);
                    NourishedColorSlots.addFixed(panel, NourishedColors.DIET_PANEL, "nourished.options.color.panel");
                    NourishedColorSlots.addFixed(panel, NourishedColors.DIET_TITLE, "nourished.options.color.title");
                    NourishedColorSlots.addFixed(panel, NourishedColors.DIET_TODAY, "nourished.options.color.today_text");
                    // Roles several Diet boxes draw live here once, so a shared key is never repeated across tabs. The
                    // toggle's housing, border and lever colors (toggle.housing/border/lever) are deliberately left to colors.json.
                    panel.colorTab(text("nourished.options.tab.shared"));
                    NourishedColorSlots.addFixed(panel, NourishedColors.TEXT, "nourished.options.color.text");
                    NourishedColorSlots.addFixed(panel, NourishedColors.TEXT_HEADER, "nourished.options.color.intake_header_text");
                    NourishedColorSlots.addFixed(panel, NourishedColors.TEXT_MUTED, "nourished.options.color.muted_text");
                    NourishedColorSlots.addFixed(panel, NourishedColors.BORDER, "nourished.options.color.border");
                    NourishedColorSlots.addFixed(panel, NourishedColors.DIVIDER, "nourished.options.color.divider");
                    NourishedColorSlots.addFixed(panel, NourishedColors.TOGGLE_ON, "nourished.options.color.toggle_on");
                    NourishedColorSlots.addFixed(panel, NourishedColors.TOGGLE_OFF, "nourished.options.color.toggle_off");
                })
                .build();
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
        StandardPanelBuilder panel = MarieModuleSettings.standardPanel(title, DietScreenPersistence.get(), moduleId)
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

    /**
     * The Calories box's colors: the calorie value (its text and bar fill), its label text and its border.
     * Text and border are shared roles, so editing one here changes it everywhere it is drawn (see the Shared tab).
     */
    public static void caloriesColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.CALORIES_HEADER, "nourished.options.color.header_text");
        NourishedColorSlots.addFixed(panel, NourishedColors.CALORIE_VALUE, "nourished.options.color.calorie");
        NourishedColorSlots.addFixed(panel, NourishedColors.CALORIES_BORDER, "nourished.options.color.border");
    }

    /** The Recent Meals box's colors: header, meal text and border (shared roles, as for {@link #caloriesColors}). */
    public static void recentMealsColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.RECENT_MEALS_HEADER, "nourished.options.color.header_text");
        NourishedColorSlots.addFixed(panel, NourishedColors.RECENT_MEALS_TEXT, "nourished.options.color.text");
        NourishedColorSlots.addFixed(panel, NourishedColors.RECENT_MEALS_BORDER, "nourished.options.color.border");
    }

    /** The Eat More box's colors: header and border (shared roles, as for {@link #caloriesColors}). */
    public static void eatMoreColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.EAT_MORE_HEADER, "nourished.options.color.header_text");
        NourishedColorSlots.addFixed(panel, NourishedColors.EAT_MORE_BORDER, "nourished.options.color.border");
    }

    /** The Balance box's colors: its header (a shared role, as for {@link #recentMealsColors}) and one per balance state. */
    public static void balanceColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_HEADER, "nourished.options.color.header_text");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_BALANCED, "nourished.options.color.balanced");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_LOW, "nourished.options.color.balance_low");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_EXCESS, "nourished.options.color.balance_excess");
    }

    /** The Active Effects box's colors: helpful and harmful effect lines. */
    public static void effectsColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.ACTIVE_EFFECTS_HEADER, "nourished.options.color.header_text");
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
