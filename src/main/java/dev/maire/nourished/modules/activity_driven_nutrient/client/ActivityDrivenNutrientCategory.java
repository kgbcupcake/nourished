package dev.maire.nourished.modules.activity_driven_nutrient.client;

import dev.maire.nourished.client.config.categories.widgets.StyledChipTextEntry;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
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

            List<AbstractConfigListEntry> sprintEntries = new ArrayList<>();
            sprintEntries.add(booleanToggle(eb, "sprintEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            sprintEntries.add(numericSlider(eb, "sprintDecayBoost", SPRINT_BOOST_DEFAULT, 0.0d, 0.1d, SPRINT_BOOST_DEFAULT, false, v -> { }));
            sprintEntries.add(new ActivityModuleColorRowEntry("sprint"));
            category.addEntry(eb.startSubCategory(Component.literal("Sprinting"), sprintEntries).setExpanded(false).build());

            List<AbstractConfigListEntry> swimEntries = new ArrayList<>();
            swimEntries.add(booleanToggle(eb, "swimEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            swimEntries.add(numericSlider(eb, "swimDecayBoost", SWIM_BOOST_DEFAULT, 0.0d, 0.1d, SWIM_BOOST_DEFAULT, false, v -> { }));
            swimEntries.add(new ActivityModuleColorRowEntry("swim"));
            category.addEntry(eb.startSubCategory(Component.literal("Swimming"), swimEntries).setExpanded(false).build());

            List<AbstractConfigListEntry> miningEntries = new ArrayList<>();
            miningEntries.add(booleanToggle(eb, "miningEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            miningEntries.add(numericSlider(eb, "miningCostPerBlock", MINING_COST_DEFAULT, 0.0d, 0.1d, MINING_COST_DEFAULT, false, v -> { }));
            miningEntries.add(new ActivityModuleColorRowEntry("mining"));
            category.addEntry(eb.startSubCategory(Component.literal("Mining"), miningEntries).setExpanded(false).build());

            List<AbstractConfigListEntry> combatEntries = new ArrayList<>();
            combatEntries.add(booleanToggle(eb, "combatEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            combatEntries.add(numericSlider(eb, "combatCostPerKill", COMBAT_COST_DEFAULT, 0.0d, 0.5d, COMBAT_COST_DEFAULT, false, v -> { }));
            combatEntries.add(new ActivityModuleColorRowEntry("combat"));
            category.addEntry(eb.startSubCategory(Component.literal("Combat"), combatEntries).setExpanded(false).build());

            List<AbstractConfigListEntry> starvationEntries = new ArrayList<>();
            starvationEntries.add(booleanToggle(eb, "starvationEnabled", UNSYNCED_DISPLAY_VALUE, false, v -> { }));
            starvationEntries.add(numericSlider(eb, "starvationPenalty", STARVATION_PENALTY_DEFAULT, 0.0d, 0.5d, STARVATION_PENALTY_DEFAULT, false, v -> { }));
            starvationEntries.add(new ActivityModuleColorRowEntry("starvation"));
            category.addEntry(eb.startSubCategory(Component.literal("Starvation"), starvationEntries).setExpanded(false).build());
        } else {
            ActivityDrivenNutrientConfig config = ActivityDrivenNutrientConfig.get();
            category.addEntry(booleanToggle(eb, "enabled", config.enabled(), true, config::setEnabled));

            List<AbstractConfigListEntry> sprintEntries = new ArrayList<>();
            sprintEntries.add(booleanToggle(eb, "sprintEnabled", config.sprintEnabled(), true, config::setSprintEnabled));
            sprintEntries.add(numericSlider(eb, "sprintDecayBoost", config.sprintDecayBoost(), 0.0d, 0.1d, SPRINT_BOOST_DEFAULT, true, config::setSprintDecayBoost));
            sprintEntries.add(new ActivityModuleColorRowEntry("sprint"));
            category.addEntry(eb.startSubCategory(Component.literal("Sprinting"), sprintEntries).setExpanded(false).build());

            List<AbstractConfigListEntry> swimEntries = new ArrayList<>();
            swimEntries.add(booleanToggle(eb, "swimEnabled", config.swimEnabled(), true, config::setSwimEnabled));
            swimEntries.add(numericSlider(eb, "swimDecayBoost", config.swimDecayBoost(), 0.0d, 0.1d, SWIM_BOOST_DEFAULT, true, config::setSwimDecayBoost));
            swimEntries.add(new ActivityModuleColorRowEntry("swim"));
            category.addEntry(eb.startSubCategory(Component.literal("Swimming"), swimEntries).setExpanded(false).build());

            List<AbstractConfigListEntry> miningEntries = new ArrayList<>();
            miningEntries.add(booleanToggle(eb, "miningEnabled", config.miningEnabled(), true, config::setMiningEnabled));
            miningEntries.add(numericSlider(eb, "miningCostPerBlock", config.miningCostPerBlock(), 0.0d, 0.1d, MINING_COST_DEFAULT, true, config::setMiningCostPerBlock));
            miningEntries.add(new ActivityModuleColorRowEntry("mining"));
            category.addEntry(eb.startSubCategory(Component.literal("Mining"), miningEntries).setExpanded(false).build());

            List<AbstractConfigListEntry> combatEntries = new ArrayList<>();
            combatEntries.add(booleanToggle(eb, "combatEnabled", config.combatEnabled(), true, config::setCombatEnabled));
            combatEntries.add(numericSlider(eb, "combatCostPerKill", config.combatCostPerKill(), 0.0d, 0.5d, COMBAT_COST_DEFAULT, true, config::setCombatCostPerKill));
            combatEntries.add(new ActivityModuleColorRowEntry("combat"));
            category.addEntry(eb.startSubCategory(Component.literal("Combat"), combatEntries).setExpanded(false).build());

            List<AbstractConfigListEntry> starvationEntries = new ArrayList<>();
            starvationEntries.add(booleanToggle(eb, "starvationEnabled", config.starvationEnabled(), true, config::setStarvationEnabled));
            starvationEntries.add(numericSlider(eb, "starvationPenalty", config.starvationPenalty(), 0.0d, 0.5d, STARVATION_PENALTY_DEFAULT, true, config::setStarvationPenalty));
            starvationEntries.add(new ActivityModuleColorRowEntry("starvation"));
            category.addEntry(eb.startSubCategory(Component.literal("Starvation"), starvationEntries).setExpanded(false).build());
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
