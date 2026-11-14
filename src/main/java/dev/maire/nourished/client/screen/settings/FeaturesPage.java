package dev.maire.nourished.client.screen.settings;

import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.maire.nourished.client.config.NourishedConfigSharedWidgets;
import dev.maire.nourished.config.NourishedConfig;

import java.util.List;

import static dev.maire.nourished.client.screen.settings.SettingsCommit.editable;
import static dev.maire.nourished.client.screen.settings.SettingsCommit.t;

/** Module on/off switches, grouped into tabs. */
final class FeaturesPage {

    private static final String P = "nourished.screen.settings.features.";

    private FeaturesPage() {}

    static MarieComponent build() {
        NourishedConfig cfg = NourishedConfig.get();
        MarieToolbox.PanelBuilder b = MarieToolbox.panel(t(P + "title"));

        b.tab(t(P + "core"));
        modules(b, cfg, List.of("enableDecay", "enableSourceApplication", "enableEffects", "enableSleepBonus"));
        if (!SettingsCommit.hidden("enableDiminishingReturns")) {
            b.toggle(t(P + "diminishingReturns"), cfg::enableDiminishingReturns, cfg::setEnableDiminishingReturns, SettingsCommit::commit)
                    .defaultValue(SettingsCommit.flag("food_memory.enableDiminishingReturns", true))
                    .enabledWhen(() -> editable("enableDiminishingReturns"));
        }
        b.resetTab();

        b.tab(t(P + "food"));
        modules(b, cfg, List.of("enableRawFoodPenalty", "enableGutHealth", "enableBlockHeavySources", "enableBlockLightSource"));
        if (!SettingsCommit.hidden("heavySourcePropertyThreshold")) {
            b.intSlider(t(P + "heavyThreshold"), cfg::heavySourcePropertyThreshold, cfg::setHeavySourcePropertyThreshold,
                            1, 20, 1, "", SettingsCommit::commit)
                    .defaultValue(SettingsCommit.number("modules.heavySourcePropertyThreshold", 6))
                    .enabledWhen(() -> editable("heavySourcePropertyThreshold")
                            && (cfg.isModuleEnabled("enableBlockHeavySources") || cfg.isModuleEnabled("enableBlockLightSource")));
        }
        b.resetTab();

        b.tab(t(P + "feedback"));
        modules(b, cfg, List.of("enableHUD", "enableToasts", "enableCriticalToasts", "enableFoodTooltips", "enableCalorieHistory"));
        b.resetTab();

        b.tab(t(P + "extras"));
        modules(b, cfg, List.of("enableSynergies", "enableMilestones", "enableSeasonHooks", "enableAbsorptionModifiers"));
        b.resetTab();

        return b.build();
    }

    private static void modules(MarieToolbox.PanelBuilder b, NourishedConfig cfg, List<String> keys) {
        for (String key : keys) {
            if (SettingsCommit.hidden(key) || !cfg.moduleToggles().containsKey(key)) {
                continue;
            }
            b.toggle(NourishedConfigSharedWidgets.moduleToggleTitle(key).getString(),
                            () -> cfg.isModuleEnabled(key), v -> cfg.setModuleEnabled(key, v), SettingsCommit::commit)
                    .defaultValue(SettingsCommit.flag("modules." + key, true))
                    .enabledWhen(() -> editable(key) && parentOn(cfg, key));
        }
    }

    private static boolean parentOn(NourishedConfig cfg, String key) {
        return !"enableCriticalToasts".equals(key) || cfg.isModuleEnabled("enableToasts");
    }
}
