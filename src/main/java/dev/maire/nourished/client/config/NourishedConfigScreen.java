package dev.maire.nourished.client.config;

import dev.maire.nourished.client.config.categories.AdvancedCategory;
import dev.maire.nourished.client.config.categories.CalorieHistoryCategory;
import dev.maire.nourished.client.config.categories.CompatibilityCategory;
import dev.maire.nourished.client.config.categories.DietScreenCategory;
import dev.maire.nourished.client.config.categories.EffectsCategory;
import dev.maire.nourished.client.config.categories.FoodValuesCategory;
import dev.maire.nourished.client.config.categories.GeneralCategory;
import dev.maire.nourished.client.config.categories.HudAndDisplayCategory;
import dev.maire.nourished.client.config.categories.ModulesCategory;
import dev.maire.nourished.client.config.categories.NutrientsCategory;
import dev.maire.nourished.client.config.categories.PresetsCategory;
import dev.maire.nourished.client.config.categories.ScannerCategory;
import dev.maire.nourished.client.config.categories.ThresholdCategory;
import dev.marie.framework.color.ColorRegistry;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.nutrition.FoodValueRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.core.nutrition.curve.NutrientCurveRegistry;
import dev.maire.nourished.core.reload.NourishedReloadHelper;
import dev.maire.nourished.modules.RawFood.core.RawFoodConfig;
import dev.maire.nourished.modules.activity_driven_nutrient.client.ActivityDrivenNutrientCategory;
import dev.maire.nourished.modules.activity_driven_nutrient.core.ActivityDrivenNutrientConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class NourishedConfigScreen {

    private NourishedConfigScreen() {}

    public static Screen create(Screen parent) {
        NourishedConfig config = NourishedConfig.get();
        NourishedClientConfig client = NourishedClientConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.nourished.title"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        Map<String, PendingOverride> decayOverrides = new LinkedHashMap<>();
        Map<String, PendingOverride> criticalOverrides = new LinkedHashMap<>();
        Map<String, PendingCurvePreset> curvePresetPending = new LinkedHashMap<>();
        Map<String, FoodValuePending> foodValuePending = new LinkedHashMap<>();
        Map<String, CompatPending> compatPending = new LinkedHashMap<>();
        Map<String, AtomicBoolean> modulePending = new LinkedHashMap<>();

        PresetsCategory.addPresetsCategory(builder, entryBuilder, parent);
        ModulesCategory.addModulesCategory(config, builder, entryBuilder, modulePending);
        GeneralCategory.addGeneralCategory(config, builder, entryBuilder);
        ThresholdCategory.addThresholdCategory(config, builder, entryBuilder);
        EffectsCategory.addEffectsCategory(config, builder, entryBuilder);
        HudAndDisplayCategory.addHudAndDisplayCategory(config, client, builder, entryBuilder, parent);
        DietScreenCategory.addDietScreenCategory(client, builder, entryBuilder);
        NutrientsCategory.addNutrientsCategory(config, builder, entryBuilder, decayOverrides, criticalOverrides, curvePresetPending);
        FoodValuesCategory.addFoodValuesCategory(builder, entryBuilder, foodValuePending);
        ScannerCategory.addScannerCategory(config, builder, entryBuilder);
        AdvancedCategory.addAdvancedCategory(config, builder, entryBuilder, modulePending);
        CompatibilityCategory.addCompatibilityCategory(config, builder, entryBuilder, compatPending);
        ActivityDrivenNutrientCategory.addActivityDrivenNutrientCategory(client, builder, entryBuilder);
        CalorieHistoryCategory.addCalorieHistoryCategory(config, client, builder, entryBuilder, modulePending);

        builder.setSavingRunnable(() -> {
            for (Map.Entry<String, PendingOverride> entry : decayOverrides.entrySet()) {
                ModConfigSpec.DoubleValue value = config.nutrientDecayRateOverrides().get(entry.getKey());
                if (value == null) continue;
                value.set(entry.getValue().enabled.get() ? entry.getValue().value.get() : -1.0d);
            }
            for (Map.Entry<String, PendingOverride> entry : criticalOverrides.entrySet()) {
                ModConfigSpec.DoubleValue value = config.nutrientCriticalThresholdOverrides().get(entry.getKey());
                if (value == null) continue;
                value.set(entry.getValue().enabled.get() ? entry.getValue().value.get() : -1.0d);
            }
            for (Map.Entry<String, PendingCurvePreset> entry : curvePresetPending.entrySet()) {
                NutrientCurveRegistry.setFromConfigScreen(entry.getKey(), entry.getValue().presetId.get());
            }
            for (Map.Entry<String, FoodValuePending> entry : foodValuePending.entrySet()) {
                FoodValuePending fv = entry.getValue();
                FoodValueRegistry.setCategory(
                        entry.getKey(),
                        fv.protein.get(),
                        fv.carbs.get(),
                        fv.fats.get(),
                        fv.vitamins.get(),
                        fv.hydration.get()
                );
            }
            for (Map.Entry<String, CompatPending> entry : compatPending.entrySet()) {
                String modid = entry.getKey();
                CompatPending cp = entry.getValue();
                ModConfigSpec.BooleanValue codeValue = config.compatCodeToggles().get(modid);
                ModConfigSpec.BooleanValue tagValue = config.compatTagToggles().get(modid);
                if (codeValue != null) codeValue.set(cp.codeCompat.get());
                if (tagValue != null) tagValue.set(cp.tagCompat.get());
            }
            for (Map.Entry<String, AtomicBoolean> entry : modulePending.entrySet()) {
                config.setModuleEnabled(entry.getKey(), entry.getValue().get());
            }
            // Dependency guards for module toggles with parent modules.
            AtomicBoolean toastsPending = modulePending.get("enableToasts");
            if (toastsPending != null && !toastsPending.get()) {
                config.setModuleEnabled("enableCriticalToasts", false);
            }
            FoodValueRegistry.save();
            ColorRegistry.save();
            EffectRegistry.save();
            NutrientRegistry.save();
            NutrientCurveRegistry.save();
            RawFoodConfig.save();
            NourishedClientConfig.saveNow();
            NourishedConfig.saveNow();
            NourishedConfig.syncModuleCache();
            // Only persist this registry locally when we're the authoritative copy (singleplayer/
            // LAN host). On a remote server isSynced() is also true, but the in-memory values are
            // the server's, not the player's own - saving them locally would silently overwrite the
            // player's real settings with whatever the connected server happens to be running.
            MinecraftServer integratedServer = Minecraft.getInstance().getSingleplayerServer();
            if (integratedServer != null) {
                if (ActivityDrivenNutrientConfig.isSynced()) {
                    ActivityDrivenNutrientConfig.saveNow();
                }
                NourishedReloadHelper.reloadAndBroadcast(integratedServer);
            }
        });
        builder.setAlwaysShowTabs(true);
        builder.setAfterInitConsumer(NourishedConfigLeftCardsLayout::apply);
        return builder.build();
    }

    public static final class PendingOverride {
        public final AtomicBoolean enabled;
        public final AtomicReference<Double> value;

        public PendingOverride(boolean enabled, double value) {
            this.enabled = new AtomicBoolean(enabled);
            this.value = new AtomicReference<>(value);
        }
    }

    public static final class PendingCurvePreset {
        public final AtomicReference<String> presetId;

        public PendingCurvePreset(@Nullable String initial) {
            this.presetId = new AtomicReference<>(initial);
        }
    }

    public static final class FoodValuePending {
        public final AtomicReference<Float> protein;
        public final AtomicReference<Float> carbs;
        public final AtomicReference<Float> fats;
        public final AtomicReference<Float> vitamins;
        public final AtomicReference<Float> hydration;

        public FoodValuePending(float protein, float carbs, float fats, float vitamins, float hydration) {
            this.protein = new AtomicReference<>(protein);
            this.carbs = new AtomicReference<>(carbs);
            this.fats = new AtomicReference<>(fats);
            this.vitamins = new AtomicReference<>(vitamins);
            this.hydration = new AtomicReference<>(hydration);
        }
    }

    public static final class CompatPending {
        public final AtomicBoolean codeCompat;
        public final AtomicBoolean tagCompat;

        public CompatPending(boolean codeCompat, boolean tagCompat) {
            this.codeCompat = new AtomicBoolean(codeCompat);
            this.tagCompat = new AtomicBoolean(tagCompat);
        }
    }
}
