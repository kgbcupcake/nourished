package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;

public final class ThresholdCategory {
    private ThresholdCategory() {}
    public static void addThresholdCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb
    ) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.thresholds"));

        if (!NourishedLockRegistry.isLocked("criticalThreshold")) {
            AbstractConfigListEntry<?> entry = buildDoubleSlider(
                    eb,
                    Component.translatable("config.nourished.criticalThreshold"),
                    config.criticalThreshold(),
                    0.0d,
                    1.0d,
                    0.25d,
                    config::setCriticalThreshold,
                    Component.translatable("config.nourished.criticalThreshold.desc")
            );
            category.addEntry(entry);
        }

        if (!NourishedLockRegistry.isLocked("lowThreshold")) {
            AbstractConfigListEntry<?> entry = buildDoubleSlider(
                    eb,
                    Component.translatable("config.nourished.lowThreshold"),
                    config.lowThreshold(),
                    0.0d,
                    1.0d,
                    0.40d,
                    config::setLowThreshold,
                    Component.translatable("config.nourished.lowThreshold.desc")
            );
            category.addEntry(entry);
        }

        if (!NourishedLockRegistry.isLocked("excessThreshold")) {
            AbstractConfigListEntry<?> entry = buildDoubleSlider(
                    eb,
                    Component.translatable("config.nourished.excessThreshold"),
                    config.excessThreshold(),
                    0.0d,
                    1.0d,
                    0.90d,
                    config::setExcessThreshold,
                    Component.translatable("config.nourished.excessThreshold.desc")
            );
            category.addEntry(entry);
        }

        if (!NourishedLockRegistry.isLocked("bonusEffectThreshold")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.bonusEffectThreshold"),
                            config.bonusEffectThreshold(),
                            0.0d,
                            1.0d,
                            0.75d,
                            config::setBonusEffectThreshold,
                            Component.translatable("config.nourished.bonusEffectThreshold.desc")
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("penaltyEffectThreshold")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.penaltyEffectThreshold"),
                            config.penaltyEffectThreshold(),
                            0.0d,
                            1.0d,
                            0.25d,
                            config::setPenaltyEffectThreshold,
                            Component.translatable("config.nourished.penaltyEffectThreshold.desc")
                    )
            );
        }

        addReloadButton(category, eb, false);
    }
}
