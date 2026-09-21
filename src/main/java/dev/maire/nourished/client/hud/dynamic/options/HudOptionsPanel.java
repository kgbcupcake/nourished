package dev.maire.nourished.client.hud.dynamic.options;

import dev.maire.nourished.client.colors.NourishedColorSlots;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.component.MarieComponent;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import net.minecraft.network.chat.Component;

/**
 * The Nutrient HUD's tabbed options panel, built through {@link MarieModuleSettings#standardPanel}, so it matches every other module window. Every option is
 * a getter/setter over a value that already exists — a {@link NourishedClientConfig} field, or the
 * HUD panel's own UI-state record via MariesLib's shared module rows — so nothing is stored here and
 * the config file keeps working. Config-backed options write in memory on every change and persist
 * with {@link NourishedClientConfig#saveNow}.
 */
public final class HudOptionsPanel {

    private static final double PERCENT_STEP = 0.01d;
    /** The config default of {@code hudBackgroundOpacity} (204/255), which "Reset This Tab" restores. */
    private static final double DEFAULT_OPACITY = 204.0d / 255.0d;

    private HudOptionsPanel() {}

    /**
     * @param panelId         the HUD panel's persisted UI-state key (the one its scale/padding/position live under)
     * @param resetTextOffset puts the panel's own text offset back to zero for "Reset Positions"
     */
    public static MarieComponent build(String panelId, Runnable resetTextOffset) {
        var ui = UiStatePersistence.get();
        return MarieModuleSettings.standardPanel(text("nourished.hud.nutrientPanel.label"), ui, panelId)
                .opacity(() -> cc().hudBackgroundOpacity(), v -> cc().setHudBackgroundOpacity(v), DEFAULT_OPACITY)
                .textBrightness(() -> cc().hudTextBrightness(), v -> cc().setHudTextBrightness(v))
                .iconBrightness(() -> cc().hudIconBrightness(), v -> cc().setHudIconBrightness(v))
                .backgroundShade(() -> cc().hudBackgroundShade(), v -> cc().setHudBackgroundShade(v))
                .borderOpacity(() -> cc().hudBorderOpacity(), v -> cc().setHudBorderOpacity(v))
                .borderShade(() -> cc().hudBorderShade(), v -> cc().setHudBorderShade(v))
                .onCommit(NourishedClientConfig::saveNow)
                .onReset(resetTextOffset)
                .layoutRows(p -> p.toggle(text("nourished.options.hud.vertical_layout"),
                                () -> cc().hudVerticalLayout(), v -> cc().setHudVerticalLayout(v), NourishedClientConfig::saveNow)
                        .defaultValue(false))
                .behaviorRows(p -> p.section(text("nourished.options.hud.section.visibility"))
                        .toggle(text("nourished.options.hud.reveal_on_gain"),
                                () -> cc().hudRevealOnNutrientGain(), v -> cc().setHudRevealOnNutrientGain(v), NourishedClientConfig::saveNow)
                            .defaultValue(true)
                        .slider(text("nourished.options.hud.hide_above"),
                                () -> cc().hudHideAboveThreshold(), v -> cc().setHudHideAboveThreshold(v), 0.0d, 1.0d, PERCENT_STEP, NourishedClientConfig::saveNow)
                            .defaultValue(1.0d)
                        .slider(text("nourished.options.hud.show_above"),
                                () -> cc().hudShowAboveThreshold(), v -> cc().setHudShowAboveThreshold(v), 0.0d, 1.0d, PERCENT_STEP, NourishedClientConfig::saveNow)
                            .defaultValue(1.0d)
                            // "Show above" only re-reveals bars the hide rule hid, so it does nothing while hide is off (1.0).
                            .enabledWhen(() -> cc().hudHideAboveThreshold() < 1.0d)
                        .toggle(text("nourished.options.hud.show_empty_bars"),
                                () -> cc().hudShowZeroBars(), v -> cc().setHudShowZeroBars(v), NourishedClientConfig::saveNow)
                            .defaultValue(false))
                .extraTabs(panel -> {
                    panel.colorTab(text("config.marieslib.moduleoptions.tab.colors"));
                    NourishedColorSlots.addNutrients(panel);
                    NourishedColorSlots.addFixed(panel, NourishedColors.HUD_PANEL, "nourished.options.color.background");
                    NourishedColorSlots.addFixed(panel, NourishedColors.NUTRIENT_HUD_TEXT, "nourished.options.color.label_text");
                })
                .build();
    }

    private static NourishedClientConfig cc() {
        return NourishedClientConfig.get();
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
