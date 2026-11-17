package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.categories.widgets.ModuleToggleListEntry;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.addReloadButtonEntries;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.isMultiplayer;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.moduleToggleDescription;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.moduleToggleTitle;

public final class CalorieHistoryCategory {
    private CalorieHistoryCategory() {}
    public static void addCalorieHistoryCategory(
            NourishedConfig config,
            NourishedClientConfig client,
            ConfigBuilder builder,
            ConfigEntryBuilder eb,
            Map<String, AtomicBoolean> modulePending
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.modules"));
        List<AbstractConfigListEntry> entries = new ArrayList<>();

        AtomicBoolean calorieHistoryPending = modulePending.get("enableCalorieHistory");
        if (calorieHistoryPending != null && !NourishedLockRegistry.isLocked("enableCalorieHistory")) {
            var calorieHistoryEntry = new ModuleToggleListEntry(
                    moduleToggleTitle("enableCalorieHistory"),
                    moduleToggleDescription("enableCalorieHistory"),
                    calorieHistoryPending,
                    "other",
                    null,
                    modulePending);
            boolean editable = !(NourishedLockRegistry.isServerOnly("enableCalorieHistory") && isMultiplayer());
            if (!editable) {
                calorieHistoryEntry.setEditable(false);
            }
            entries.add(calorieHistoryEntry);
        }

        if (!NourishedLockRegistry.isLocked("calorieHistoryRetentionDays")) {
            entries.add(
                    eb.startIntSlider(Component.translatable("config.nourished.calorieHistoryRetentionDays"), config.calorieHistoryRetentionDays(), 1, 90)
                            .setDefaultValue(7)
                            .setTextGetter(v -> Component.literal(String.valueOf(v)))
                            .setSaveConsumer(config::setCalorieHistoryRetentionDays)
                            .build()
            );
        }


        addReloadButtonEntries(entries, eb, false);

        category.addEntry(
                eb.startSubCategory(Component.translatable("config.nourished.category.calorieHistory"), entries)
                        .setExpanded(false)
                        .build()
        );
    }
}
