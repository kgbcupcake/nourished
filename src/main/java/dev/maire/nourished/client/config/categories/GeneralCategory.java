package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.NourishedConfigSharedWidgets;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.config.NourishedLockRegistry;
import dev.maire.nourished.core.nutrition.curve.NutrientCurvePreset;
import dev.marie.framework.tracking.RespawnValueBehavior;
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
                            config::setDecayRate
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("decayIntervalTicks")) {
            category.addEntry(
                    eb.startIntSlider(Component.translatable("config.nourished.decayIntervalTicks"), config.decayIntervalTicks(), 20, 72000)
                            .setDefaultValue(1200)
                            .setTextGetter(v -> Component.literal(v + " ticks"))
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
                            config::setStartingNutrientValue
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("deathNutritionBehavior")) {
            category.addEntry(
                    eb.startStrField(
                                    Component.translatable("config.nourished.deathNutritionBehavior"),
                                    config.deathNutritionBehaviorConfigId()
                            )
                            .setDefaultValue(RespawnValueBehavior.PRESERVE.configId())
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
                            config::setNutrientGainScale
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
                            config::setNutrientGainPerBiteMax
                    )
            );
        }

        if (!NourishedLockRegistry.isLocked("enableNutrientCurves")) {
            category.addEntry(
                    eb.startBooleanToggle(Component.translatable("config.nourished.enableNutrientCurves"), config.enableNutrientCurves())
                            .setDefaultValue(false)
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
