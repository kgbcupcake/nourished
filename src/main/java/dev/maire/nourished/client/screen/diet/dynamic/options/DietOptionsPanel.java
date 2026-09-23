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
                // No Layout-tab size sliders: the panel and its sub-boxes are all directly drag-resizable
                // in edit mode, which makes a separate percentage slider per box redundant.
                .behaviorRows(p -> {
                    p.toggle(text("nourished.options.diet.drag_bars"),
                                    () -> cc().dietBarDragEnabled(), v -> cc().setDietBarDragEnabled(v), save)
                            .defaultValue(true);
                    p.button(text("nourished.options.diet.reset_bar_order"), text("nourished.options.diet.reset_caption"),
                            () -> cc().resetDietBarOrder(), save);
                    // Per-row Move/Hide/Reset now lives on each row's own panel again, reached via the
                    // hub's "Intake" group picker (see DietScreen#intakeGroupEntry) — no duplicate
                    // reset-position buttons needed here.
                    p.section(text("nourished.options.hud.section.visibility"));
                    // Show/hide for Recent Meals, Eat More Of, Active Effects, Calories and Balance now lives on
                    // each box's own gear-icon panel (its Behavior tab's Hide Window toggle) instead of here.
                    // Read only when the inventory screen opens (ClientEvents#onScreenInit), so a change applies on the next open.
                    p.toggle(text("nourished.options.diet.show_inventory_button"),
                                    () -> cc().showDietScreenButton(), v -> cc().setShowDietScreenButton(v), save)
                            .defaultValue(true);
                    p.endSection();
                    // The consolidated "Reset This Module" button MarieModuleSettings#standardPanel
                    // now always adds at the top of the Layout tab covers these Behavior-tab rows too
                    // (see OptionLayout#allRows) — no separate tab-scoped reset needed here anymore.
                })
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
        return forModule(title, moduleId, hasBars, hasIcons, hasHeader, true, colors);
    }

    /**
     * Same, also choosing whether "Move Text" appears at all — false for a box whose body content
     * already moves under some other toggle (e.g. Recent Meals, whose row names travel with "Move
     * Bars" instead — see {@link dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent#render}),
     * leaving nothing for "Move Text" to actually move.
     */
    public static MarieComponent forModule(String title, String moduleId, boolean hasBars, boolean hasIcons, boolean hasHeader,
                                           boolean hasMoveText, java.util.function.Consumer<MarieToolbox.PanelBuilder> colors) {
        return forModule(title, moduleId, hasBars, hasIcons, hasHeader, hasMoveText, true, colors);
    }

    /**
     * Same, also choosing whether "Hide Text" appears — false for a box whose text serves no purpose
     * hiding on its own (e.g. Eat More Of's suggestion text, which the "Hide Window" toggle already
     * covers). Every Diet sub-box panel also drops the Padding slider (Layout tab): every box is
     * directly drag-resizable and every piece of its content individually movable, so a separate
     * padding-percentage knob is redundant.
     */
    public static MarieComponent forModule(String title, String moduleId, boolean hasBars, boolean hasIcons, boolean hasHeader,
                                           boolean hasMoveText, boolean hasHideText, java.util.function.Consumer<MarieToolbox.PanelBuilder> colors) {
        return forModule(title, moduleId, hasBars, hasIcons, hasHeader, hasMoveText, hasHideText, colors, null);
    }

    /**
     * Same, with the Style tab's "Text size" row relabeled to {@code textSizeLabelKey} ({@code null}:
     * the standard "Text size" label) — for a box whose persisted text scale only ever drives its
     * header (e.g. Recent Meals, whose row names follow Bar size instead — see {@link
     * dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent#render}), where the
     * generic "Text size" label would misleadingly suggest it also resizes the box's body content.
     */
    public static MarieComponent forModule(String title, String moduleId, boolean hasBars, boolean hasIcons, boolean hasHeader,
                                           boolean hasMoveText, boolean hasHideText, java.util.function.Consumer<MarieToolbox.PanelBuilder> colors,
                                           String textSizeLabelKey) {
        StandardPanelBuilder panel = MarieModuleSettings.standardPanel(title, DietScreenPersistence.get(), moduleId)
                .storedBrightness()
                .withoutPadding();
        if (textSizeLabelKey != null) {
            panel.textSizeLabel(textSizeLabelKey);
        }
        if (!hasBars) {
            panel.withoutBars();
        }
        if (!hasIcons) {
            panel.withoutIcons();
        }
        if (hasHeader) {
            panel.withHeader();
        }
        if (!hasMoveText) {
            panel.withoutMoveText();
        }
        if (!hasHideText) {
            panel.withoutHideText();
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

    /** The Balance box's colors: its header (a shared role, as for {@link #recentMealsColors}), one per balance state, and its border. */
    public static void balanceColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_HEADER, "nourished.options.color.header_text");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_BALANCED, "nourished.options.color.balanced");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_LOW, "nourished.options.color.balance_low");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_EXCESS, "nourished.options.color.balance_excess");
        NourishedColorSlots.addFixed(panel, NourishedColors.BALANCE_BORDER, "nourished.options.color.border");
    }

    /** The Active Effects box's colors: helpful and harmful effect lines, and its border. */
    public static void effectsColors(MarieToolbox.PanelBuilder panel) {
        NourishedColorSlots.addFixed(panel, NourishedColors.ACTIVE_EFFECTS_HEADER, "nourished.options.color.header_text");
        NourishedColorSlots.addFixed(panel, NourishedColors.EFFECT_BENEFICIAL, "nourished.options.color.beneficial");
        NourishedColorSlots.addFixed(panel, NourishedColors.EFFECT_HARMFUL, "nourished.options.color.harmful");
        NourishedColorSlots.addFixed(panel, NourishedColors.ACTIVE_EFFECTS_BORDER, "nourished.options.color.border");
    }

    /**
     * Options panel shared by every Intake Breakdown row — the rows are structurally identical (icon,
     * label, bar, percent, arrow), differing only in which nutrient a given slot currently shows, so
     * one panel definition is reused per row id rather than one bespoke panel per nutrient. Reached via
     * the hub's "Intake" group picker (see {@code DietScreen#intakeGroupEntry}), one popup per row.
     * Per-row bar-fill color is intentionally NOT exposed here: that stays driven by the existing
     * global per-nutrient color system (see the "Nutrients" colors tab), same as the HUD's own bars.
     */
    public static MarieComponent forIntakeBar(String title, String moduleId) {
        return forModule(title, moduleId, true, true, false);
    }

    /**
     * The hub editor window's own colors — its chrome, independent of the boxes it edits. A
     * colors-only panel (no Layout/Behavior/Style tabs — there's no position/size/text of its own to
     * move, just its four chrome colors), reached from the hub's own "Editor" sidebar entry rather
     * than a per-box one.
     */
    public static MarieComponent editorPanel() {
        MarieToolbox.PanelBuilder panel = MarieToolbox.panel(text("nourished.options.diet.editor_title"))
                .colorTab(text("config.marieslib.moduleoptions.tab.colors"));
        NourishedColorSlots.addFixed(panel, NourishedColors.HUB_BACKGROUND, "nourished.options.color.hub_background");
        NourishedColorSlots.addFixed(panel, NourishedColors.HUB_BORDER, "nourished.options.color.hub_border");
        NourishedColorSlots.addFixed(panel, NourishedColors.HUB_TITLE, "nourished.options.color.hub_title");
        NourishedColorSlots.addFixed(panel, NourishedColors.HUB_ACCENT, "nourished.options.color.hub_accent");
        return panel.build();
    }

    private static NourishedClientConfig cc() {
        return NourishedClientConfig.get();
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
