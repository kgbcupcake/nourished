package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.NourishedConfigSharedWidgets;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import dev.maire.nourished.core.nutrition.curve.NutrientCurvePreset;
import dev.marie.MariesLib.tracking.DeathNutritionBehavior;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;

public final class GeneralCategory {
    private GeneralCategory() {}
    public static void addGeneralCategory(NourishedConfig config, ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.nourished.category.general"));

        if (!NourishedLockRegistry.isLocked("decayRate")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.decayRate"),
                            config.decayRate(),
                            0.0d,
                            1.0d,
                            0.1d,
                            config::setDecayRate,
                            Component.translatable("config.nourished.decayRate.desc")
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("decayIntervalTicks")) {
            category.addEntry(
                    eb.startIntSlider(Component.translatable("config.nourished.decayIntervalTicks"), config.decayIntervalTicks(), 20, 72000)
                            .setDefaultValue(1200)
                            .setTextGetter(v -> Component.literal(v + " ticks"))
                            .setTooltip(Component.translatable("config.nourished.decayIntervalTicks.desc"))
                            .setSaveConsumer(config::setDecayIntervalTicks)
                            .build()
            );
        }

        if (!NourishedLockRegistry.isLocked("startingNutrientValue")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.startingNutrientValue"),
                            config.startingNutrientValue(),
                            0.0d,
                            1.0d,
                            0.5d,
                            config::setStartingNutrientValue,
                            Component.translatable("config.nourished.startingNutrientValue.desc")
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("deathNutritionBehavior")) {
            category.addEntry(
                    eb.startStrField(
                                    Component.translatable("config.nourished.deathNutritionBehavior"),
                                    config.deathNutritionBehaviorConfigId()
                            )
                            .setDefaultValue(DeathNutritionBehavior.PRESERVE.configId())
                            .setTooltip(Component.translatable("config.nourished.deathNutritionBehavior.desc"))
                            .setSaveConsumer(config::setDeathNutritionBehavior)
                            .build()
            );
        }

        if (!NourishedLockRegistry.isLocked("nutrientGainScale")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.nutrientGainScale"),
                            config.nutrientGainScale(),
                            0.5d,
                            20.0d,
                            5.0d,
                            config::setNutrientGainScale,
                            Component.translatable("config.nourished.nutrientGainScale.desc")
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("nutrientGainPerBiteMax")) {
            category.addEntry(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.nutrientGainPerBiteMax"),
                            config.nutrientGainPerBiteMax(),
                            0.05d,
                            1.0d,
                            0.2d,
                            config::setNutrientGainPerBiteMax,
                            Component.translatable("config.nourished.nutrientGainPerBiteMax.desc")
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("enableNutrientCurves")) {
            category.addEntry(
                    eb.startBooleanToggle(Component.translatable("config.nourished.enableNutrientCurves"), config.enableNutrientCurves())
                            .setDefaultValue(false)
                            .setTooltip(Component.translatable("config.nourished.enableNutrientCurves.desc"))
                            .setSaveConsumer(config::setEnableNutrientCurves)
                            .build()
            );
        }

        if (!NourishedLockRegistry.isLocked("defaultCurvePreset")) {
            category.addEntry(
                    eb.startEnumSelector(
                                    Component.translatable("config.nourished.defaultCurvePreset"),
                                    NutrientCurvePreset.class,
                                    parseDefaultCurvePreset(config.defaultCurvePreset())
                            )
                            .setDefaultValue(NutrientCurvePreset.FLAT)
                            .setTooltip(Component.translatable("config.nourished.defaultCurvePreset.desc"))
                            .setSaveConsumer(preset -> config.setDefaultCurvePreset(preset.name()))
                            .build()
            );
        }

        addReloadButton(category, eb, false);
    }
    private static NutrientCurvePreset parseDefaultCurvePreset(String presetId) {
        if (presetId == null) {
            return NutrientCurvePreset.FLAT;
        }
        try {
            return NutrientCurvePreset.valueOf(presetId);
        } catch (IllegalArgumentException e) {
            return NutrientCurvePreset.FLAT;
        }
    }
}
