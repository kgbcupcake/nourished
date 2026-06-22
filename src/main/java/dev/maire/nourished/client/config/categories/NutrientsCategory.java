package dev.maire.nourished.client.config.categories;

import dev.maire.nourished.client.config.NourishedConfigScreen.PendingCurvePreset;
import dev.maire.nourished.client.config.NourishedConfigScreen.PendingOverride;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.curve.NutrientCurveDef;
import dev.maire.nourished.core.nutrition.curve.NutrientCurvePreset;
import dev.maire.nourished.core.nutrition.curve.NutrientCurveRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.*;

public final class NutrientsCategory {
    private NutrientsCategory() {}
    public static void addNutrientsCategory(
            NourishedConfig config,
            ConfigBuilder builder,
            ConfigEntryBuilder eb,
            Map<String, PendingOverride> decayOverrides,
            Map<String, PendingOverride> criticalOverrides,
            Map<String, PendingCurvePreset> curvePresetPending
    ) {
        ConfigCategory nutrients = builder.getOrCreateCategory(Component.translatable("config.nourished.category.nutrients"));
        for (String key : NutrientRegistry.getKeys()) {
            double decayRaw = overrideRaw(config.nutrientDecayRateOverrides().get(key));
            double criticalRaw = overrideRaw(config.nutrientCriticalThresholdOverrides().get(key));

            PendingOverride decay = new PendingOverride(decayRaw >= 0d, decayRaw >= 0d ? decayRaw : config.decayRate());
            PendingOverride critical = new PendingOverride(criticalRaw >= 0d, criticalRaw >= 0d ? criticalRaw : config.criticalThreshold());
            decayOverrides.put(key, decay);
            criticalOverrides.put(key, critical);

            NutrientCurveDef existingCurve = NutrientCurveRegistry.get(key);
            String currentPresetId = existingCurve != null ? existingCurve.presetId() : null;
            PendingCurvePreset curvePending = new PendingCurvePreset(currentPresetId);
            curvePresetPending.put(key, curvePending);

            List<AbstractConfigListEntry> entries = new ArrayList<>();
            entries.add(
                    eb.startBooleanToggle(Component.translatable("config.nourished.nutrient.overrideDecay", prettyKey(key)), decay.enabled.get())
                            .setDefaultValue(false)
                            .setSaveConsumer(decay.enabled::set)
                            .build()
            );
            entries.add(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.nutrient.decayRate", prettyKey(key)),
                            decay.value.get(),
                            0.0d,
                            1.0d,
                            config.decayRate(),
                            decay.value::set
                    )
            );
            entries.add(
                    eb.startBooleanToggle(Component.translatable("config.nourished.nutrient.overrideCritical", prettyKey(key)), critical.enabled.get())
                            .setDefaultValue(false)
                            .setSaveConsumer(critical.enabled::set)
                            .build()
            );
            entries.add(
                    buildDoubleSlider(
                            eb,
                            Component.translatable("config.nourished.nutrient.criticalThreshold", prettyKey(key)),
                            critical.value.get(),
                            0.0d,
                            1.0d,
                            config.criticalThreshold(),
                            critical.value::set
                    )
            );
            entries.add(
                    eb.startStringDropdownMenu(
                                    Component.translatable("config.nourished.nutrient.curvePreset", prettyKey(key)),
                                    currentPresetId == null ? "" : currentPresetId
                            )
                            .setSelections(curvePresetDropdownOptions())
                            .setDefaultValue("")
                            .setTooltip(Component.translatable("config.nourished.nutrient.curvePreset.desc"))
                            .setSaveConsumer(curvePending.presetId::set)
                            .build()
            );

            nutrients.addEntry(
                    eb.startSubCategory(Component.translatable("config.nourished.nutrient.category", prettyKey(key)), entries)
                            .setExpanded(false)
                            .build()
            );
        }

        addReloadButton(nutrients, eb, false);
    }
    private static List<String> curvePresetDropdownOptions() {
        List<String> options = new ArrayList<>();
        options.add("");
        for (NutrientCurvePreset preset : NutrientCurvePreset.values()) {
            options.add(preset.name());
        }
        return options;
    }
    private static double overrideRaw(ModConfigSpec.DoubleValue value) {
        return value != null ? value.get() : -1.0d;
    }
}
