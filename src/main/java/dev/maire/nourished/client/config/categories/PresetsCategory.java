package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.NourishedImportExportButtonsWidget;
import dev.maire.nourished.client.config.NourishedPresetsWidget;
import dev.maire.nourished.client.config.NourishedConfigSharedWidgets;
import dev.maire.nourished.config.NourishedPresetRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.addReloadButton;

public final class PresetsCategory {
    private PresetsCategory() {}
    public static void addPresetsCategory(ConfigBuilder builder, ConfigEntryBuilder eb, Screen reopenParent) {
        NourishedPresetRegistry.ensureBuiltInFilesOnDisk();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.presets"));
        category.addEntry(new NourishedPresetsWidget(reopenParent));
        addReloadButton(category, eb, false);
        category.addEntry(new NourishedImportExportButtonsWidget(reopenParent));
    }
}
