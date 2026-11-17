package dev.maire.nourished.client.screen.settings;

import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.maire.nourished.config.NourishedConfig;

import java.util.List;
import java.util.function.BooleanSupplier;

import static dev.maire.nourished.client.screen.settings.SettingsCommit.number;
import static dev.maire.nourished.client.screen.settings.SettingsCommit.t;

/** Decay, thresholds, food gains and food variety numbers. */
final class BalancePage {

    private static final String P = "nourished.screen.settings.balance.";
    private static final List<String> DEATH_IDS = List.of("preserve", "reset_to_starting", "vanilla_half");

    private BalancePage() {}

    static MarieComponent build() {
        NourishedConfig cfg = NourishedConfig.get();
        BooleanSupplier on = () -> SettingsCommit.editable("balance");
        MarieToolbox.PanelBuilder b = MarieToolbox.panel(t(P + "title"));

        b.tab(t(P + "decay"));
        b.decimalSlider(t(P + "decayRate"), cfg::decayRate, cfg::setDecayRate, 0, 1, 0.005, 3, "", SettingsCommit::commit)
                .defaultValue(number("general.decayRate", 0.1)).enabledWhen(on);
        b.intSlider(t(P + "decayInterval"), () -> Math.max(1, cfg.decayIntervalTicks() / 20),
                        s -> cfg.setDecayIntervalTicks(s * 20), 1, 600, 5, "s", SettingsCommit::commit)
                .defaultValue(number("general.decayIntervalTicks", 1200) / 20.0).enabledWhen(on);
        b.resetTab();

        b.tab(t(P + "thresholds"));
        b.slider(t(P + "critical"), cfg::criticalThreshold, cfg::setCriticalThreshold, 0, 1, 0.01, SettingsCommit::commit)
                .defaultValue(number("thresholds.criticalThreshold", 0.25)).enabledWhen(on);
        b.slider(t(P + "low"), cfg::lowThreshold, cfg::setLowThreshold, 0, 1, 0.01, SettingsCommit::commit)
                .defaultValue(number("thresholds.lowThreshold", 0.4)).enabledWhen(on);
        b.slider(t(P + "excess"), cfg::excessThreshold, cfg::setExcessThreshold, 0, 1, 0.01, SettingsCommit::commit)
                .defaultValue(number("thresholds.excessThreshold", 0.9)).enabledWhen(on);
        b.resetTab();

        b.tab(t(P + "gains"));
        b.decimalSlider(t(P + "gainScale"), cfg::nutrientGainScale, cfg::setNutrientGainScale, 0.5, 20, 0.5, 1, "x", SettingsCommit::commit)
                .defaultValue(number("food_gains.nutrientGainScale", 5.0)).enabledWhen(on);
        b.slider(t(P + "perBiteMax"), cfg::nutrientGainPerBiteMax, cfg::setNutrientGainPerBiteMax, 0.05, 1, 0.01, SettingsCommit::commit)
                .defaultValue(number("food_gains.nutrientGainPerBiteMax", 0.2)).enabledWhen(on);
        b.resetTab();

        BooleanSupplier variety = () -> on.getAsBoolean() && cfg.enableDiminishingReturns();
        b.tab(t(P + "variety"));
        b.intSlider(t(P + "memoryWindow"), cfg::memoryWindowMinutes, cfg::setMemoryWindowMinutes, 1, 120, 1, "min", SettingsCommit::commit)
                .defaultValue(number("food_memory.memoryWindowMinutes", 60)).enabledWhen(variety);
        b.decimalSlider(t(P + "noveltyBonus"), cfg::noveltyBonus, cfg::setNoveltyBonus, 1, 2, 0.05, 2, "x", SettingsCommit::commit)
                .defaultValue(number("food_memory.noveltyBonus", 1.25)).enabledWhen(variety);
        b.slider(t(P + "floor"), cfg::diminishingFloor, cfg::setDiminishingFloor, 0, 1, 0.05, SettingsCommit::commit)
                .defaultValue(number("food_memory.diminishingFloor", 0.15)).enabledWhen(variety);
        b.resetTab();

        b.tab(t(P + "player"));
        b.slider(t(P + "starting"), cfg::startingNutrientValue, cfg::setStartingNutrientValue, 0, 1, 0.05, SettingsCommit::commit)
                .defaultValue(number("general.startingNutrientValue", 0.5)).enabledWhen(on);
        String[] labels = DEATH_IDS.stream().map(id -> t(P + "death." + id)).toArray(String[]::new);
        b.cycle(t(P + "death"), labels, () -> Math.max(0, DEATH_IDS.indexOf(cfg.deathNutritionBehaviorConfigId())),
                        i -> cfg.setDeathNutritionBehavior(DEATH_IDS.get(i)), SettingsCommit::commit)
                .defaultValue(Math.max(0, DEATH_IDS.indexOf(SettingsCommit.text("general.deathNutritionBehavior", "preserve"))))
                .enabledWhen(on);
        b.resetTab();

        return b.build();
    }
}
