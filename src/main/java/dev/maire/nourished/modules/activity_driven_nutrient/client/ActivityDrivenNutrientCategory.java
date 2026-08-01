package dev.maire.nourished.modules.activity_driven_nutrient.client;

import dev.maire.nourished.client.config.categories.widgets.StyledChipTextEntry;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.addReloadButton;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.buildDoubleSlider;

public final class ActivityDrivenNutrientCategory {
    private ActivityDrivenNutrientCategory() {}

    /** Placeholder shown for every toggle when the SERVER config hasn't synced yet — never a real value. */
    private static final boolean UNSYNCED_DISPLAY_VALUE = true;

    // Mirrors ActivityDrivenNutrientConfig's own defaults — shown read-only while unsynced, since the
    // real config values aren't safe to read yet (see the guard below).
    private static final double MINING_COST_DEFAULT = 0.0005d;
    private static final double COMBAT_COST_DEFAULT = 0.01d;
    private static final double SPRINT_BOOST_DEFAULT = 0.0004d;
    private static final double SWIM_BOOST_DEFAULT = 0.0004d;
    private static final double STARVATION_PENALTY_DEFAULT = 0.02d;

    public static void addActivityDrivenNutrientCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("config.nourished.category.activityDrivenNutrient"));

        boolean synced = ActivityDrivenNutrientConfig.isSynced();
        if (!synced) {
            category.addEntry(new StyledChipTextEntry(
                    Component.translatable("config.nourished.activityDrivenNutrient.notConnected"),
                    0xFFCC4444));

            // Unsynced: ActivityDrivenNutrientConfig's ConfigValue#get() throws before the SERVER
            // config has loaded, so every entry must use the display placeholder and a no-op save —
            // the real config instance is never touched on this path.
            category.addEntry(booleanToggle(eb, "enabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            category.addEntry(booleanToggle(eb, "sprintEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            category.addEntry(booleanToggle(eb, "swimEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            category.addEntry(booleanToggle(eb, "miningEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            category.addEntry(booleanToggle(eb, "combatEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            category.addEntry(booleanToggle(eb, "starvationEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            category.addEntry(numericSlider(eb, "miningCostPerBlock", MINING_COST_DEFAULT, 0.0d, 0.1d, MINING_COST_DEFAULT, false, v -> { }));
            category.addEntry(numericSlider(eb, "combatCostPerKill", COMBAT_COST_DEFAULT, 0.0d, 0.5d, COMBAT_COST_DEFAULT, false, v -> { }));
            category.addEntry(numericSlider(eb, "sprintDecayBoost", SPRINT_BOOST_DEFAULT, 0.0d, 0.1d, SPRINT_BOOST_DEFAULT, false, v -> { }));
            category.addEntry(numericSlider(eb, "swimDecayBoost", SWIM_BOOST_DEFAULT, 0.0d, 0.1d, SWIM_BOOST_DEFAULT, false, v -> { }));
            category.addEntry(numericSlider(eb, "starvationPenalty", STARVATION_PENALTY_DEFAULT, 0.0d, 0.5d, STARVATION_PENALTY_DEFAULT, false, v -> { }));
        } else {
            ActivityDrivenNutrientConfig config = ActivityDrivenNutrientConfig.get();
            category.addEntry(booleanToggle(eb, "enabled", config.enabled(), true, config::setEnabled));
            category.addEntry(booleanToggle(eb, "sprintEnabled", config.sprintEnabled(), true, config::setSprintEnabled));
            category.addEntry(booleanToggle(eb, "swimEnabled", config.swimEnabled(), true, config::setSwimEnabled));
            category.addEntry(booleanToggle(eb, "miningEnabled", config.miningEnabled(), true, config::setMiningEnabled));
            category.addEntry(booleanToggle(eb, "combatEnabled", config.combatEnabled(), true, config::setCombatEnabled));
            category.addEntry(booleanToggle(eb, "starvationEnabled", config.starvationEnabled(), true, config::setStarvationEnabled));
            category.addEntry(numericSlider(eb, "miningCostPerBlock", config.miningCostPerBlock(), 0.0d, 0.1d, MINING_COST_DEFAULT, true, config::setMiningCostPerBlock));
            category.addEntry(numericSlider(eb, "combatCostPerKill", config.combatCostPerKill(), 0.0d, 0.5d, COMBAT_COST_DEFAULT, true, config::setCombatCostPerKill));
            category.addEntry(numericSlider(eb, "sprintDecayBoost", config.sprintDecayBoost(), 0.0d, 0.1d, SPRINT_BOOST_DEFAULT, true, config::setSprintDecayBoost));
            category.addEntry(numericSlider(eb, "swimDecayBoost", config.swimDecayBoost(), 0.0d, 0.1d, SWIM_BOOST_DEFAULT, true, config::setSwimDecayBoost));
            category.addEntry(numericSlider(eb, "starvationPenalty", config.starvationPenalty(), 0.0d, 0.5d, STARVATION_PENALTY_DEFAULT, true, config::setStarvationPenalty));
        }

        addReloadButton(category, eb, false);
    }

    private static AbstractConfigListEntry booleanToggle(
            ConfigEntryBuilder eb,
            String key,
            boolean value,
            boolean editable,
            Consumer<Boolean> saveConsumer
    ) {
        AbstractConfigListEntry entry = eb.startBooleanToggle(
                        Component.translatable("config.nourished.activityDrivenNutrient." + key),
                        value)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.nourished.activityDrivenNutrient." + key + ".desc"))
                .setSaveConsumer(saveConsumer::accept)
                .build();
        entry.setEditable(editable);
        return entry;
    }

    private static AbstractConfigListEntry numericSlider(
            ConfigEntryBuilder eb,
            String key,
            double value,
            double min,
            double max,
            double defaultValue,
            boolean editable,
            DoubleConsumer saveConsumer
    ) {
        AbstractConfigListEntry entry = buildDoubleSlider(
                eb,
                Component.translatable("config.nourished.activityDrivenNutrient." + key),
                value,
                min,
                max,
                defaultValue,
                saveConsumer,
                Component.translatable("config.nourished.activityDrivenNutrient." + key + ".desc")
        );
        entry.setEditable(editable);
        return entry;
    }
}
