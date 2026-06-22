package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.categories.widgets.ModuleToggleListEntry;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.addReloadButton;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.isMultiplayer;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.moduleToggleDescription;
import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.moduleToggleTitle;

public final class AdvancedCategory {
    private AdvancedCategory() {}
    public static void addAdvancedCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb,
            Map<String, AtomicBoolean> modulePending
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.advanced"));

        AtomicBoolean debugLogPending = modulePending.get("enableDebugLogging");
        if (debugLogPending != null && !NourishedLockRegistry.isLocked("enableDebugLogging")) {
            var debugEntry = new ModuleToggleListEntry(
                    moduleToggleTitle("enableDebugLogging"),
                    moduleToggleDescription("enableDebugLogging"),
                    debugLogPending,
                    "other",
                    null,
                    modulePending);
            boolean editable = !(NourishedLockRegistry.isServerOnly("enableDebugLogging") && isMultiplayer());
            if (!editable) {
                debugEntry.setEditable(false);
            }
            category.addEntry(debugEntry);
        }

        if (!NourishedLockRegistry.isLocked("calorieDisplayMax")) {
            category.addEntry(
                    eb.startIntSlider(Component.translatable("config.nourished.calorieDisplayMax"), config.calorieDisplayMax(), 100, 100000)
                            .setDefaultValue(2000)
                            .setTextGetter(v -> Component.literal(String.valueOf(v)))
                            .setTooltip(Component.translatable("config.nourished.calorieDisplayMax.desc"))
                            .setSaveConsumer(config::setCalorieDisplayMax)
                            .build()
            );
        }

        addReloadButton(category, eb, false);
    }
}
