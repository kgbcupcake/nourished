package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.FoodScannerWidget;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;

public final class ScannerCategory {
    private ScannerCategory() {}
    public static void addScannerCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.scanner"));

        if (!NourishedLockRegistry.isLocked("scanner.enableRecipeInheritance")) {
            category.addEntry(
                    eb.startBooleanToggle(
                                    Component.translatable("config.nourished.scanner.enableRecipeInheritance"),
                                    config.scannerEnableRecipeInheritance()
                            )
                            .setDefaultValue(true)
                            .setTooltip(Component.translatable("config.nourished.scanner.enableRecipeInheritance.desc"))
                            .setSaveConsumer(config::setScannerEnableRecipeInheritance)
                            .build()
            );
        }

        if (!NourishedLockRegistry.isLocked("scanner.confidenceSpreadThreshold")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.scanner.confidenceSpreadThreshold"),
                            config.scannerConfidenceSpreadThreshold(),
                            0.0d,
                            20.0d,
                            3.0d,
                            config::setScannerConfidenceSpreadThreshold,
                            Component.translatable("config.nourished.scanner.confidenceSpreadThreshold.desc")
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("scanner.multiNutrientInheritanceThreshold")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.scanner.multiNutrientInheritanceThreshold"),
                            config.multiNutrientInheritanceThreshold(),
                            0.0d,
                            1.0d,
                            0.20d,
                            config::setMultiNutrientInheritanceThreshold,
                            Component.translatable("config.nourished.scanner.multiNutrientInheritanceThreshold.desc")
                    )
            );
        }

        category.addEntry(new FoodScannerWidget());
        addReloadButton(category, eb, false);
    }
}
