package dev.maire.nourished.config;

import com.google.gson.JsonObject;
import dev.maire.nourished.compat.ModCompat;
import dev.maire.nourished.core.effect.EffectRegistry;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collections;
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
    private final ModConfigSpec.BooleanValue enableSleepBonus;

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
    private final Map<String, ModConfigSpec.BooleanValue> moduleToggles = new LinkedHashMap<>();

    private NourishedConfig(ModConfigSpec.Builder builder) {
        JsonObject defaults = ConfigDefaultsLoader.loadOrEmpty("/data/nourished/config/common_defaults.json");

        builder.push("modules");
        enableDecay = defineModuleToggle(builder, "enableDecay", "When false, NutritionDecayHandler does nothing", ConfigDefaultsLoader.getBoolean(defaults, "enableDecay", true));
        defineModuleToggle(builder, "enableNutritionEating", "When false, nutrition-only eating bypass at full hunger is disabled. Items tagged nourished:meal never use this bypass.", ConfigDefaultsLoader.getBoolean(defaults, "enableNutritionEating", true));
        defineModuleToggle(builder, "enableCalorieSaturationBlock", "When true, eating normal food is canceled when the calorie bar is full and vanilla hunger is full (canAlwaysEat items are unaffected).", ConfigDefaultsLoader.getBoolean(defaults, "enableCalorieSaturationBlock", true));
        enableEffects = defineModuleToggle(builder, "enableEffects", "When false, status effects from nutrition are not applied", ConfigDefaultsLoader.getBoolean(defaults, "enableEffects", true));
        enableHUD = defineModuleToggle(builder, "enableHUD", "When false, the nutrition HUD overlay is hidden", ConfigDefaultsLoader.getBoolean(defaults, "enableHUD", true));
        enableToasts = defineModuleToggle(builder, "enableToasts", "When false, NourishedToastManager never queues toasts", ConfigDefaultsLoader.getBoolean(defaults, "enableToasts", true));
        enableFoodTooltips = defineModuleToggle(builder, "enableFoodTooltips", "When false, food tooltips do not show nutrient info", ConfigDefaultsLoader.getBoolean(defaults, "enableFoodTooltips", true));
        enableCalorieTracking = defineModuleToggle(builder, "enableCalorieTracking", "When false, DietData.addCalories() is never called and calorie display is hidden", ConfigDefaultsLoader.getBoolean(defaults, "enableCalorieTracking", true));
        enableDietScreen = defineModuleToggle(builder, "enableDietScreen", "When false, the keybind to open DietScreen does nothing", ConfigDefaultsLoader.getBoolean(defaults, "enableDietScreen", true));
        enableCriticalToasts = defineModuleToggle(builder, "enableCriticalToasts", "Separate from enableToasts, controls only the critical-threshold toast specifically", ConfigDefaultsLoader.getBoolean(defaults, "enableCriticalToasts", true));
        enableSleepBonus = defineModuleToggle(builder, "enableSleepBonus", "When true, sleeping with all nutrient bars above 50% grants Regeneration I for 30 seconds", ConfigDefaultsLoader.getBoolean(defaults, "enableSleepBonus", true));
        defineModuleToggle(builder, "enablePSStaminaUsage", "When false, Peak Stamina nutrition hook does not apply the stamina_usage attribute modifier", ConfigDefaultsLoader.getBoolean(defaults, "enablePSStaminaUsage", true));
        defineModuleToggle(builder, "enablePSPenaltyDecay", "When false, Peak Stamina nutrition hook does not apply the penalty_decay_multiplier attribute modifier", ConfigDefaultsLoader.getBoolean(defaults, "enablePSPenaltyDecay", true));
        defineModuleToggle(builder, "enablePSExhaustionDuration", "When false, Peak Stamina nutrition hook does not apply the exhaustion_duration_multiplier attribute modifier", ConfigDefaultsLoader.getBoolean(defaults, "enablePSExhaustionDuration", true));
        defineModuleToggle(builder, "enableSOLDiversityHealth", "When false, Spice of Life: Onion hook does not apply the high-diversity max health bonus on minecraft:max_health", ConfigDefaultsLoader.getBoolean(defaults, "enableSOLDiversityHealth", false));
        defineModuleToggle(builder, "enableSOLDiversityPenalty", "When false, Spice of Life: Onion hook does not apply the low-diversity max health penalty on minecraft:max_health", ConfigDefaultsLoader.getBoolean(defaults, "enableSOLDiversityPenalty", true));
        defineModuleToggle(builder, "enableLSOThermalResistance", "When true, nutrition level affects LSO thermal resistance attribute", ConfigDefaultsLoader.getBoolean(defaults, "enableLSOThermalResistance", true));
        defineModuleToggle(builder, "enableLSOBrokenHeartResilience", "When true, high nutrition boosts LSO broken heart resilience", ConfigDefaultsLoader.getBoolean(defaults, "enableLSOBrokenHeartResilience", true));
        defineModuleToggle(builder, "enableLSOThirstSaturation", "When true, eating nutritious food adds a small LSO thirst saturation bonus", ConfigDefaultsLoader.getBoolean(defaults, "enableLSOThirstSaturation", true));
        defineModuleToggle(builder, "enableSynergies", "When false, nutrient and food synergy checks are skipped", ConfigDefaultsLoader.getBoolean(defaults, "enableSynergies", true));
        defineModuleToggle(builder, "enableMilestones", "When false, milestone checks are skipped", ConfigDefaultsLoader.getBoolean(defaults, "enableMilestones", true));
        defineModuleToggle(builder, "enableSeasonHooks", "When false, season hook modifiers are ignored", ConfigDefaultsLoader.getBoolean(defaults, "enableSeasonHooks", true));
        defineModuleToggle(builder, "enableAbsorptionModifiers", "When false, absorption modifier hooks are ignored", ConfigDefaultsLoader.getBoolean(defaults, "enableAbsorptionModifiers", true));
        builder.pop();

        builder.push("general");
        decayRate = builder
                .comment("Base decay rate per interval for all nutrients")
                .defineInRange("decayRate", ConfigDefaultsLoader.getDouble(defaults, "decayRate", defaultDecayRateFromRegistry()), 0.0d, 1.0d);
        decayIntervalTicks = builder
                .comment("Ticks between nutrient decay applications")
                .defineInRange("decayIntervalTicks", ConfigDefaultsLoader.getInt(defaults, "decayIntervalTicks", 1200), 1, Integer.MAX_VALUE);
        startingNutrientValue = builder
                .comment("Initial fill (0-1) for every nutrient bar on new diet data. Default 0.5 avoids starting debuffs when rules use the usual below-0.25 thresholds.")
                .defineInRange("startingNutrientValue", ConfigDefaultsLoader.getDouble(defaults, "startingNutrientValue", 0.5d), 0.0d, 1.0d);
        builder.pop();

        builder.push("thresholds");
        criticalThreshold = builder
                .comment("Nutrient level below which critical effects trigger")
                .defineInRange("criticalThreshold", ConfigDefaultsLoader.getDouble(defaults, "criticalThreshold", defaultCriticalThresholdFromRegistry()), 0.0d, 1.0d);
        lowThreshold = builder
                .comment("Nutrient level below which low warnings appear")
                .defineInRange("lowThreshold", ConfigDefaultsLoader.getDouble(defaults, "lowThreshold", defaultLowThresholdFromRegistry()), 0.0d, 1.0d);
        excessThreshold = builder
                .comment("Nutrient level above which excess warnings appear")
                .defineInRange("excessThreshold", ConfigDefaultsLoader.getDouble(defaults, "excessThreshold", defaultExcessThresholdFromRegistry()), 0.0d, 1.0d);
        builder.pop();

        builder.push("effects");
        bonusEffectThreshold = builder
                .comment("Nutrient level above which bonus effects are applied")
                .defineInRange("bonusEffectThreshold", ConfigDefaultsLoader.getDouble(defaults, "bonusEffectThreshold", defaultBonusEffectThresholdFromRegistry()), 0.0d, 1.0d);
        penaltyEffectThreshold = builder
                .comment("Nutrient level below which penalty effects are applied")
                .defineInRange("penaltyEffectThreshold", ConfigDefaultsLoader.getDouble(defaults, "penaltyEffectThreshold", defaultPenaltyEffectThresholdFromRegistry()), 0.0d, 1.0d);
        defaultEffectDurationTicks = builder
                .comment("Default duration in ticks for nutrition effects")
                .defineInRange("defaultEffectDurationTicks", ConfigDefaultsLoader.getInt(defaults, "defaultEffectDurationTicks", defaultEffectDurationFromRegistry()), 20, 72000);
        builder.pop();

        builder.push("advanced");
        calorieDisplayMax = builder
                .comment("Maximum calorie value for display purposes")
                .defineInRange("calorieDisplayMax", ConfigDefaultsLoader.getInt(defaults, "calorieDisplayMax", 2000), 100, 100000);
        builder.pop();

        builder.push("food_memory");
        memoryWindowMinutes = builder
                .comment("How long (in minutes) before a food memory entry expires.")
                .defineInRange("memoryWindowMinutes", ConfigDefaultsLoader.getInt(defaults, "memoryWindowMinutes", 20), 1, 120);
        memoryWindowCount = builder
                .comment("Maximum number of distinct food entries tracked in memory.")
                .defineInRange("memoryWindowCount", ConfigDefaultsLoader.getInt(defaults, "memoryWindowCount", 10), 1, 50);
        diminishingFloor = builder
                .comment("Minimum multiplier for heavily repeated foods. 0.15 = 15% credit floor.")
                .defineInRange("diminishingFloor", ConfigDefaultsLoader.getDouble(defaults, "diminishingFloor", 0.15), 0.0, 1.0);
        diminishingSteps = builder
                .comment("(Legacy) Multiplier curve by eat count. Ignored when using logistic curve.")
                .defineList("diminishingSteps", java.util.List.of(1.0, 0.7, 0.4, 0.15),
                    e -> e instanceof Double d && d >= 0.0 && d <= 1.0);

        // Logistic curve parameters
        diminishingSteepness = builder
                .comment("Steepness of the logistic diminishing curve. Higher = sharper transition.")
                .defineInRange("diminishingSteepness", ConfigDefaultsLoader.getDouble(defaults, "diminishingSteepness", 0.8), 0.1, 3.0);
        diminishingMidpoint = builder
                .comment("Midpoint of the logistic curve (eat count where multiplier = 0.5).")
                .defineInRange("diminishingMidpoint", ConfigDefaultsLoader.getDouble(defaults, "diminishingMidpoint", 3.0), 1.0, 10.0);

        // Streak settings
        streakWindowMs = builder
                .comment("Time window (ms) for streak detection. Eating same food within window increases penalty faster.")
                .defineInRange("streakWindowMs", ConfigDefaultsLoader.getInt(defaults, "streakWindowMs", 300000), 10000, 1800000);
        streakWeight = builder
                .comment("Multiplier for eat count increment when within streak window (e.g., 2.0 = double penalty).")
                .defineInRange("streakWeight", ConfigDefaultsLoader.getDouble(defaults, "streakWeight", 2.0), 1.0, 5.0);

        // Novelty settings
        noveltyBonus = builder
                .comment("Maximum multiplier bonus for completely novel foods.")
                .defineInRange("noveltyBonus", ConfigDefaultsLoader.getDouble(defaults, "noveltyBonus", 1.25), 1.0, 2.0);
        noveltyDecayCap = builder
                .comment("Decayed eat count at which novelty bonus fully disappears.")
                .defineInRange("noveltyDecayCap", ConfigDefaultsLoader.getDouble(defaults, "noveltyDecayCap", 5.0), 1.0, 20.0);

        // Nutritional debt settings
        debtThreshold = builder
                .comment("Category eat count threshold above which nutritional debt kicks in.")
                .defineInRange("debtThreshold", ConfigDefaultsLoader.getDouble(defaults, "debtThreshold", 4.0), 1.0, 20.0);
        debtDecayRate = builder
                .comment("Rate at which neglected categories decay when debt is triggered.")
                .defineInRange("debtDecayRate", ConfigDefaultsLoader.getDouble(defaults, "debtDecayRate", 0.003), 0.0, 0.05);

        // Debug
        debugMemoryLogging = builder
                .comment("Enable detailed debug logging for food memory system (development only).")
                .define("debugMemoryLogging", ConfigDefaultsLoader.getBoolean(defaults, "debugMemoryLogging", false));
        builder.pop();

        builder.push("food_gains");
        nutrientGainScale = builder
                .comment("Multiplies tag-based nutrient burst before per-bite cap. Lower = slower bar fill.")
                .defineInRange("nutrientGainScale", ConfigDefaultsLoader.getDouble(defaults, "nutrientGainScale", 5.0d), 0.5d, 20.0d);
        nutrientGainPerBiteMax = builder
                .comment("Max fraction (0-1) one bite can add to a single bar from tag-based foods (not food overrides).")
                .defineInRange("nutrientGainPerBiteMax", ConfigDefaultsLoader.getDouble(defaults, "nutrientGainPerBiteMax", 0.2d), 0.05d, 1.0d);
        builder.pop();

        builder.push("scanner");
        scannerEnableRecipeInheritance = builder
                .comment("Enable recipe ingredient inheritance signal for food classification (requires server access)")
                .define("enableRecipeInheritance", ConfigDefaultsLoader.getBoolean(defaults, "scannerEnableRecipeInheritance", true));
        scannerConfidenceSpreadThreshold = builder
                .comment("Minimum spread between dominant and secondary scores for confident classification (higher = stricter)")
                .defineInRange("confidenceSpreadThreshold", ConfigDefaultsLoader.getDouble(defaults, "scannerConfidenceSpreadThreshold", 3.0d), 0.0d, 20.0d);
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
        ModuleCache.refresh();
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
        ModuleCache.refresh();
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

    public boolean enableSleepBonus() {
        return enableSleepBonus.get();
    }

    public void setEnableSleepBonus(boolean value) {
        enableSleepBonus.set(value);
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

    public Map<String, ModConfigSpec.BooleanValue> moduleToggles() {
        return Collections.unmodifiableMap(moduleToggles);
    }

    public boolean isModuleEnabled(String key) {
        ModConfigSpec.BooleanValue value = moduleToggles.get(key);
        return value != null && value.get();
    }

    public void setModuleEnabled(String key, boolean enabled) {
        ModConfigSpec.BooleanValue value = moduleToggles.get(key);
        if (value != null) {
            value.set(enabled);
        }
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

    private ModConfigSpec.BooleanValue defineModuleToggle(
            ModConfigSpec.Builder builder,
            String key,
            String comment,
            boolean defaultValue
    ) {
        ModConfigSpec.BooleanValue value = builder.comment(comment).define(key, defaultValue);
        moduleToggles.put(key, value);
        return value;
    }

    private static double defaultDecayRateFromRegistry() {
        return NutrientRegistry.getAll().stream()
                .findFirst()
                .map(NutrientRegistry.NutrientDef::defaultDecayRate)
                .orElse(0.1f);
    }

    private static double defaultCriticalThresholdFromRegistry() {
        return NutrientRegistry.getAll().stream()
                .findFirst()
                .map(NutrientRegistry.NutrientDef::criticalThreshold)
                .orElse(0.25f);
    }

    private static double defaultLowThresholdFromRegistry() {
        return NutrientRegistry.getAll().stream()
                .findFirst()
                .map(NutrientRegistry.NutrientDef::lowThreshold)
                .orElse(0.4f);
    }

    private static double defaultExcessThresholdFromRegistry() {
        return NutrientRegistry.getAll().stream()
                .findFirst()
                .map(NutrientRegistry.NutrientDef::excessThreshold)
                .orElse(0.9f);
    }

    private static double defaultBonusEffectThresholdFromRegistry() {
        return EffectRegistry.getAll().stream()
                .filter(def -> "all_above".equals(def.trigger()))
                .findFirst()
                .map(EffectRegistry.EffectDef::threshold)
                .orElse(0.75d);
    }

    private static double defaultPenaltyEffectThresholdFromRegistry() {
        return EffectRegistry.getAll().stream()
                .filter(def -> "below".equals(def.trigger()))
                .findFirst()
                .map(EffectRegistry.EffectDef::threshold)
                .orElse(0.25d);
    }

    private static int defaultEffectDurationFromRegistry() {
        return EffectRegistry.getAll().stream()
                .findFirst()
                .map(EffectRegistry.EffectDef::durationTicks)
                .orElse(140);
    }
}
