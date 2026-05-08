package dev.maire.nourished.config;

import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.nutrition.Nourished;
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

    // Food Memory
    private final ModConfigSpec.IntValue memoryWindowMinutes;
    private final ModConfigSpec.IntValue memoryWindowCount;
    private final ModConfigSpec.DoubleValue diminishingFloor;
    private final ModConfigSpec.ConfigValue<java.util.List<? extends Double>> diminishingSteps;

    // Logistic curve parameters (new system, replaces step-based)
    private final ModConfigSpec.DoubleValue diminishingSteepness;
    private final ModConfigSpec.DoubleValue diminishingMidpoint;

    // Streak multiplier
    private final ModConfigSpec.IntValue streakWindowMs;
    private final ModConfigSpec.DoubleValue streakWeight;

    // Graduated novelty
    private final ModConfigSpec.DoubleValue noveltyBonus;
    private final ModConfigSpec.DoubleValue noveltyDecayCap;

    // Nutritional debt
    private final ModConfigSpec.DoubleValue debtThreshold;
    private final ModConfigSpec.DoubleValue debtDecayRate;

    // Debug
    private final ModConfigSpec.BooleanValue debugMemoryLogging;

    // Tag-based food → diet bar gains (FoodNutritionRegistry.computeDietDelta)
    private final ModConfigSpec.DoubleValue nutrientGainScale;
    private final ModConfigSpec.DoubleValue nutrientGainPerBiteMax;

    // Scanner configuration
    private final ModConfigSpec.BooleanValue scannerEnableRecipeInheritance;
    private final ModConfigSpec.DoubleValue scannerConfidenceSpreadThreshold;

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
                .comment("Initial fill (0-1) for every nutrient bar on new diet data. Default 0.5 avoids starting debuffs when rules use the usual below-0.25 thresholds.")
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

        builder.push("food_memory");
        memoryWindowMinutes = builder
                .comment("How long (in minutes) before a food memory entry expires.")
                .defineInRange("memoryWindowMinutes", 20, 1, 120);
        memoryWindowCount = builder
                .comment("Maximum number of distinct food entries tracked in memory.")
                .defineInRange("memoryWindowCount", 10, 1, 50);
        diminishingFloor = builder
                .comment("Minimum multiplier for heavily repeated foods. 0.15 = 15% credit floor.")
                .defineInRange("diminishingFloor", 0.15, 0.0, 1.0);
        diminishingSteps = builder
                .comment("(Legacy) Multiplier curve by eat count. Ignored when using logistic curve.")
                .defineList("diminishingSteps", java.util.List.of(1.0, 0.7, 0.4, 0.15),
                    e -> e instanceof Double d && d >= 0.0 && d <= 1.0);

        // Logistic curve parameters
        diminishingSteepness = builder
                .comment("Steepness of the logistic diminishing curve. Higher = sharper transition.")
                .defineInRange("diminishingSteepness", 0.8, 0.1, 3.0);
        diminishingMidpoint = builder
                .comment("Midpoint of the logistic curve (eat count where multiplier = 0.5).")
                .defineInRange("diminishingMidpoint", 3.0, 1.0, 10.0);

        // Streak settings
        streakWindowMs = builder
                .comment("Time window (ms) for streak detection. Eating same food within window increases penalty faster.")
                .defineInRange("streakWindowMs", 300000, 10000, 1800000);
        streakWeight = builder
                .comment("Multiplier for eat count increment when within streak window (e.g., 2.0 = double penalty).")
                .defineInRange("streakWeight", 2.0, 1.0, 5.0);

        // Novelty settings
        noveltyBonus = builder
                .comment("Maximum multiplier bonus for completely novel foods.")
                .defineInRange("noveltyBonus", 1.25, 1.0, 2.0);
        noveltyDecayCap = builder
                .comment("Decayed eat count at which novelty bonus fully disappears.")
                .defineInRange("noveltyDecayCap", 5.0, 1.0, 20.0);

        // Nutritional debt settings
        debtThreshold = builder
                .comment("Category eat count threshold above which nutritional debt kicks in.")
                .defineInRange("debtThreshold", 4.0, 1.0, 20.0);
        debtDecayRate = builder
                .comment("Rate at which neglected categories decay when debt is triggered.")
                .defineInRange("debtDecayRate", 0.003, 0.0, 0.05);

        // Debug
        debugMemoryLogging = builder
                .comment("Enable detailed debug logging for food memory system (development only).")
                .define("debugMemoryLogging", false);
        builder.pop();

        builder.push("food_gains");
        nutrientGainScale = builder
                .comment("Multiplies tag-based nutrient burst before per-bite cap. Lower = slower bar fill.")
                .defineInRange("nutrientGainScale", 5.0d, 0.5d, 20.0d);
        nutrientGainPerBiteMax = builder
                .comment("Max fraction (0-1) one bite can add to a single bar from tag-based foods (not food overrides).")
                .defineInRange("nutrientGainPerBiteMax", 0.2d, 0.05d, 1.0d);
        builder.pop();

        builder.push("scanner");
        scannerEnableRecipeInheritance = builder
                .comment("Enable recipe ingredient inheritance signal for food classification (requires server access)")
                .define("enableRecipeInheritance", true);
        scannerConfidenceSpreadThreshold = builder
                .comment("Minimum spread between dominant and secondary scores for confident classification (higher = stricter)")
                .defineInRange("confidenceSpreadThreshold", 3.0d, 0.0d, 20.0d);
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
        for (String modid : ModCompat.getDetected().keySet()) {
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

    public int memoryWindowMinutes() {
        return memoryWindowMinutes.get();
    }

    public int memoryWindowCount() {
        return memoryWindowCount.get();
    }

    public double diminishingFloor() {
        return diminishingFloor.get();
    }

    public java.util.List<? extends Double> diminishingSteps() {
        return diminishingSteps.get();
    }

    public double diminishingSteepness() {
        return diminishingSteepness.get();
    }

    public double diminishingMidpoint() {
        return diminishingMidpoint.get();
    }

    public int streakWindowMs() {
        return streakWindowMs.get();
    }

    public double streakWeight() {
        return streakWeight.get();
    }

    public double noveltyBonus() {
        return noveltyBonus.get();
    }

    public double noveltyDecayCap() {
        return noveltyDecayCap.get();
    }

    public double debtThreshold() {
        return debtThreshold.get();
    }

    public double debtDecayRate() {
        return debtDecayRate.get();
    }

    public boolean debugMemoryLogging() {
        return debugMemoryLogging.get();
    }

    public double nutrientGainScale() {
        return nutrientGainScale.get();
    }

    public void setNutrientGainScale(double value) {
        nutrientGainScale.set(value);
    }

    public double nutrientGainPerBiteMax() {
        return nutrientGainPerBiteMax.get();
    }

    public void setNutrientGainPerBiteMax(double value) {
        nutrientGainPerBiteMax.set(value);
    }

    // Scanner config getters

    public boolean scannerEnableRecipeInheritance() {
        return scannerEnableRecipeInheritance.get();
    }

    public void setScannerEnableRecipeInheritance(boolean value) {
        scannerEnableRecipeInheritance.set(value);
    }

    public double scannerConfidenceSpreadThreshold() {
        return scannerConfidenceSpreadThreshold.get();
    }

    public void setScannerConfidenceSpreadThreshold(double value) {
        scannerConfidenceSpreadThreshold.set(value);
    }
}
