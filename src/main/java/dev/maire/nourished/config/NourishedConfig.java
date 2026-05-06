package dev.maire.nourished.config;

import dev.maire.nourished.Nourished;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server/common configuration for Nourished.
 * <p>
 * <b>Priority / Override Stack (lowest to highest):</b>
 * <ol>
 *   <li>Hardcoded Java defaults (fallback only, never relied on at runtime)</li>
 *   <li>TOML config files (nourished-common.toml, nourished-client.toml) — player/server editable</li>
 *   <li>config/nourished/*.json files — modpack creator layer, ships with the pack</li>
 *   <li>data/nourished/config/*.json datapack files — highest priority, overrides everything below</li>
 * </ol>
 * When a value exists at a higher layer it completely replaces the lower layer value.
 */
public final class NourishedConfig {

    private static NourishedConfig INSTANCE;
    private static ModConfigSpec SPEC;
    private static volatile ModConfig boundCommonConfig;

    // Module toggles
    // config.nourished.enableDecay
    // config.nourished.enableDecay.desc
    private final ModConfigSpec.BooleanValue enableDecay;
    private final ModConfigSpec.BooleanValue enableEffects;
    private final ModConfigSpec.BooleanValue enableHUD;
    // config.nourished.enableToasts
    // config.nourished.enableToasts.desc
    private final ModConfigSpec.BooleanValue enableToasts;
    // config.nourished.enableFoodTooltips
    // config.nourished.enableFoodTooltips.desc
    private final ModConfigSpec.BooleanValue enableFoodTooltips;
    // config.nourished.enableCalorieTracking
    // config.nourished.enableCalorieTracking.desc
    private final ModConfigSpec.BooleanValue enableCalorieTracking;
    // config.nourished.enableDietScreen
    // config.nourished.enableDietScreen.desc
    private final ModConfigSpec.BooleanValue enableDietScreen;
    // config.nourished.enableCriticalToasts
    // config.nourished.enableCriticalToasts.desc
    private final ModConfigSpec.BooleanValue enableCriticalToasts;

    // General
    private final ModConfigSpec.DoubleValue decayRate;
    private final ModConfigSpec.IntValue decayIntervalTicks;
    // config.nourished.startingNutrientValue
    // config.nourished.startingNutrientValue.desc
    private final ModConfigSpec.DoubleValue startingNutrientValue;

    // Thresholds
    private final ModConfigSpec.DoubleValue criticalThreshold;
    private final ModConfigSpec.DoubleValue lowThreshold;
    private final ModConfigSpec.DoubleValue excessThreshold;

    // Effects
    private final ModConfigSpec.DoubleValue bonusEffectThreshold;
    private final ModConfigSpec.DoubleValue penaltyEffectThreshold;
    // config.nourished.defaultEffectDurationTicks
    // config.nourished.defaultEffectDurationTicks.desc
    private final ModConfigSpec.IntValue defaultEffectDurationTicks;

    // Advanced
    // config.nourished.calorieDisplayMax
    // config.nourished.calorieDisplayMax.desc
    private final ModConfigSpec.IntValue calorieDisplayMax;

    private final Map<String, ModConfigSpec.DoubleValue> nutrientDecayRateOverrides;
    private final Map<String, ModConfigSpec.DoubleValue> nutrientCriticalThresholdOverrides;

    // config.nourished.compat.<modid>.enableCodeCompat
    // config.nourished.compat.<modid>.enableCodeCompat.desc
    // config.nourished.compat.<modid>.enableTagCompat
    // config.nourished.compat.<modid>.enableTagCompat.desc
    private final Map<String, ModConfigSpec.BooleanValue> compatCodeToggles = new LinkedHashMap<>();
    private final Map<String, ModConfigSpec.BooleanValue> compatTagToggles = new LinkedHashMap<>();

    private NourishedConfig(ModConfigSpec.Builder builder) {
        builder.push("modules");
        enableDecay = builder
                .comment("When false, NutritionDecayHandler does nothing")
                .define("enableDecay", true);
        enableEffects = builder
                .comment("When false, status effects from nutrition are not applied")
                .define("enableEffects", true);
        enableHUD = builder
                .comment("When false, the nutrition HUD overlay is hidden")
                .define("enableHUD", true);
        enableToasts = builder
                .comment("When false, NourishedToastManager never queues toasts")
                .define("enableToasts", true);
        enableFoodTooltips = builder
                .comment("When false, food tooltips do not show nutrient info")
                .define("enableFoodTooltips", true);
        enableCalorieTracking = builder
                .comment("When false, DietData.addCalories() is never called and calorie display is hidden")
                .define("enableCalorieTracking", true);
        enableDietScreen = builder
                .comment("When false, the keybind to open DietScreen does nothing")
                .define("enableDietScreen", true);
        enableCriticalToasts = builder
                .comment("Separate from enableToasts, controls only the critical-threshold toast specifically")
                .define("enableCriticalToasts", true);
        builder.pop();

        builder.push("general");
        decayRate = builder
                .comment("Base decay rate per interval for all nutrients")
                .defineInRange("decayRate", 0.1d, 0.0d, 1.0d);
        decayIntervalTicks = builder
                .comment("Ticks between nutrient decay applications")
                .defineInRange("decayIntervalTicks", 1200, 1, Integer.MAX_VALUE);
        startingNutrientValue = builder
                .comment("Initial value for all nutrients when a new player joins")
                .defineInRange("startingNutrientValue", 0.5d, 0.0d, 1.0d);
        builder.pop();

        builder.push("thresholds");
        criticalThreshold = builder
                .comment("Nutrient level below which critical effects trigger")
                .defineInRange("criticalThreshold", 0.25d, 0.0d, 1.0d);
        lowThreshold = builder
                .comment("Nutrient level below which low warnings appear")
                .defineInRange("lowThreshold", 0.40d, 0.0d, 1.0d);
        excessThreshold = builder
                .comment("Nutrient level above which excess warnings appear")
                .defineInRange("excessThreshold", 0.90d, 0.0d, 1.0d);
        builder.pop();

        builder.push("effects");
        bonusEffectThreshold = builder
                .comment("Nutrient level above which bonus effects are applied")
                .defineInRange("bonusEffectThreshold", 0.75d, 0.0d, 1.0d);
        penaltyEffectThreshold = builder
                .comment("Nutrient level below which penalty effects are applied")
                .defineInRange("penaltyEffectThreshold", 0.25d, 0.0d, 1.0d);
        defaultEffectDurationTicks = builder
                .comment("Default duration in ticks for nutrition effects")
                .defineInRange("defaultEffectDurationTicks", 140, 20, 72000);
        builder.pop();

        builder.push("advanced");
        calorieDisplayMax = builder
                .comment("Maximum calorie value for display purposes")
                .defineInRange("calorieDisplayMax", 2000, 100, 100000);
        builder.pop();

        nutrientDecayRateOverrides = new LinkedHashMap<>();
        nutrientCriticalThresholdOverrides = new LinkedHashMap<>();
        builder.push("nutrients");
        for (String key : NutrientRegistry.getKeys()) {
            builder.push(key);
            nutrientDecayRateOverrides.put(
                    key,
                    builder.defineInRange(key + "_decayRate", -1.0d, -1.0d, 1.0d)
            );
            nutrientCriticalThresholdOverrides.put(
                    key,
                    builder.defineInRange(key + "_criticalThreshold", -1.0d, -1.0d, 1.0d)
            );
            builder.pop();
        }
        builder.pop();

        builder.push("compat");
        for (String modid : ModCompat.DETECTED.keySet()) {
            builder.push(modid);
            compatCodeToggles.put(
                    modid,
                    builder.comment("Enable special code-level behavior for " + modid)
                            .define("enableCodeCompat", true)
            );
            compatTagToggles.put(
                    modid,
                    builder.comment("Enable nutrient tag classification for foods from " + modid)
                            .define("enableTagCompat", true)
            );
            builder.pop();
        }
        builder.pop();
    }

    public static void register(ModContainer modContainer) {
        if (INSTANCE != null) return;
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new NourishedConfig(builder);
        SPEC = builder.build();
        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static void onModConfigLoading(ModConfigEvent.Loading event) {
        ModConfig cfg = event.getConfig();
        if (!Nourished.MODID.equals(cfg.getModId()) || cfg.getType() != ModConfig.Type.COMMON) {
            return;
        }
        if (cfg.getSpec() != SPEC) {
            return;
        }
        boundCommonConfig = cfg;
    }

    /**
     * Writes {@code nourished-common.toml} after programmatic changes (for example loading a preset).
     */
    public static void saveNow() {
        ModConfig cfg = boundCommonConfig;
        if (cfg == null) {
            return;
        }
        var loaded = cfg.getLoadedConfig();
        if (loaded != null) {
            loaded.save();
        }
    }

    public static NourishedConfig get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("NourishedConfig has not been registered yet.");
        }
        return INSTANCE;
    }

    public static ModConfigSpec spec() {
        return SPEC;
    }

    public double decayRate() {
        return decayRate.get();
    }

    public void setDecayRate(double value) {
        decayRate.set(value);
    }

    public int decayIntervalTicks() {
        return decayIntervalTicks.get();
    }

    public void setDecayIntervalTicks(int value) {
        decayIntervalTicks.set(value);
    }

    public boolean enableEffects() {
        return enableEffects.get();
    }

    public void setEnableEffects(boolean value) {
        enableEffects.set(value);
    }

    public boolean enableHUD() {
        return enableHUD.get();
    }

    public void setEnableHUD(boolean value) {
        enableHUD.set(value);
    }

    public boolean enableDecay() {
        return enableDecay.get();
    }

    public void setEnableDecay(boolean value) {
        enableDecay.set(value);
    }

    public boolean enableToasts() {
        return enableToasts.get();
    }

    public void setEnableToasts(boolean value) {
        enableToasts.set(value);
    }

    public boolean enableFoodTooltips() {
        return enableFoodTooltips.get();
    }

    public void setEnableFoodTooltips(boolean value) {
        enableFoodTooltips.set(value);
    }

    public boolean enableCalorieTracking() {
        return enableCalorieTracking.get();
    }

    public void setEnableCalorieTracking(boolean value) {
        enableCalorieTracking.set(value);
    }

    public boolean enableDietScreen() {
        return enableDietScreen.get();
    }

    public void setEnableDietScreen(boolean value) {
        enableDietScreen.set(value);
    }

    public boolean enableCriticalToasts() {
        return enableCriticalToasts.get();
    }

    public void setEnableCriticalToasts(boolean value) {
        enableCriticalToasts.set(value);
    }

    public double criticalThreshold() {
        return criticalThreshold.get();
    }

    public void setCriticalThreshold(double value) {
        criticalThreshold.set(value);
    }

    public double lowThreshold() {
        return lowThreshold.get();
    }

    public void setLowThreshold(double value) {
        lowThreshold.set(value);
    }

    public double excessThreshold() {
        return excessThreshold.get();
    }

    public void setExcessThreshold(double value) {
        excessThreshold.set(value);
    }

    public double bonusEffectThreshold() {
        return bonusEffectThreshold.get();
    }

    public void setBonusEffectThreshold(double value) {
        bonusEffectThreshold.set(value);
    }

    public double penaltyEffectThreshold() {
        return penaltyEffectThreshold.get();
    }

    public void setPenaltyEffectThreshold(double value) {
        penaltyEffectThreshold.set(value);
    }

    public int defaultEffectDurationTicks() {
        return defaultEffectDurationTicks.get();
    }

    public void setDefaultEffectDurationTicks(int value) {
        defaultEffectDurationTicks.set(value);
    }

    public double startingNutrientValue() {
        return startingNutrientValue.get();
    }

    public void setStartingNutrientValue(double value) {
        startingNutrientValue.set(value);
    }

    public int calorieDisplayMax() {
        return calorieDisplayMax.get();
    }

    public void setCalorieDisplayMax(int value) {
        calorieDisplayMax.set(value);
    }

    public double decayRateFor(String key) {
        ModConfigSpec.DoubleValue value = nutrientDecayRateOverrides.get(key);
        if (value == null || value.get() < 0d) return decayRate();
        return value.get();
    }

    public double criticalThresholdFor(String key) {
        ModConfigSpec.DoubleValue value = nutrientCriticalThresholdOverrides.get(key);
        if (value == null || value.get() < 0d) return criticalThreshold();
        return value.get();
    }

    public Map<String, ModConfigSpec.DoubleValue> nutrientDecayRateOverrides() {
        return nutrientDecayRateOverrides;
    }

    public Map<String, ModConfigSpec.DoubleValue> nutrientCriticalThresholdOverrides() {
        return nutrientCriticalThresholdOverrides;
    }

    public boolean isCodeCompatEnabled(String modid) {
        ModConfigSpec.BooleanValue value = compatCodeToggles.get(modid);
        return value == null || value.get();
    }

    public boolean isTagCompatEnabled(String modid) {
        ModConfigSpec.BooleanValue value = compatTagToggles.get(modid);
        return value == null || value.get();
    }

    public Map<String, ModConfigSpec.BooleanValue> compatCodeToggles() {
        return compatCodeToggles;
    }

    public Map<String, ModConfigSpec.BooleanValue> compatTagToggles() {
        return compatTagToggles;
    }
}
