package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.EffectBuilderWidget;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.addReloadButton;

public final class EffectsCategory {
    private EffectsCategory() {}
    public static void addEffectsCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.effects"));

        if (!NourishedLockRegistry.isLocked("defaultEffectDurationTicks")) {
            category.addEntry(
                    eb.startIntSlider(Component.translatable("config.nourished.defaultEffectDurationTicks"), config.defaultEffectDurationTicks(), 20, 72000)
                            .setDefaultValue(140)
                            .setTextGetter(v -> Component.literal(v + " ticks"))
                            .setTooltip(Component.translatable("config.nourished.defaultEffectDurationTicks.desc"))
                            .setSaveConsumer(config::setDefaultEffectDurationTicks)
                            .build()
            );
        }

        category.addEntry(new EffectBuilderWidget());

        addReloadButton(category, eb, false);
    }
}
