package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.NourishedConfigScreen.CompatPending;
import dev.maire.nourished.client.config.categories.widgets.CompatTabbedListEntry;
import dev.maire.nourished.config.NourishedConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.Map;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.addReloadButton;

public final class CompatibilityCategory {
    private CompatibilityCategory() {}
    public static void addCompatibilityCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb,
            Map<String, CompatPending> compatPending
    ) {
        for (String modid : config.compatCodeToggles().keySet()) {
            compatPending.put(modid, new CompatPending(
                    config.isCodeCompatEnabled(modid),
                    config.isTagCompatEnabled(modid)
            ));
        }
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.compat"));
        category.addEntry(new CompatTabbedListEntry(config, compatPending));
        addReloadButton(category, eb, true);
    }
}
