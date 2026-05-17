package dev.maire.nourished.config;

import dev.maire.nourished.api.ApiStatus;

/**
 * Cached module toggle values for hot gameplay paths (render/tick/effect loops).
 * Refresh values after config load/reload/save.
 */
@ApiStatus.Internal
public final class ModuleCache {

    public static boolean enableDecay = true;
    public static boolean enableNutritionEating = true;
    public static boolean enableBlockHeavyMeals = false;
    /** Fallback / config value for heavy-meal nutrition threshold (see {@link ModCompatRegistry#getHeavyMealThreshold()}). */
    public static int heavyMealNutritionThreshold = 6;
    public static boolean enableBlockLightFood = false;
    public static boolean enableEffects = true;
    public static boolean enableHUD = true;
    public static boolean enableToasts = true;
    public static boolean enableFoodTooltips = true;
    public static boolean enableCalorieTracking = true;
    public static boolean enableDietScreen = true;
    public static boolean enableCriticalToasts = true;
    public static boolean enableSleepBonus = true;
    public static boolean enableRawFoodPenalty = true;
    public static boolean enablePSStaminaUsage = true;
    public static boolean enablePSPenaltyDecay = true;
    public static boolean enablePSExhaustionDuration = true;
    public static boolean enableSOLDiversityHealth = false;
    public static boolean enableSOLDiversityPenalty = true;
    public static boolean enableLSOThermalResistance = true;
    public static boolean enableLSOBrokenHeartResilience = true;
    public static boolean enableLSOThirstSaturation = true;
    public static boolean enableSynergies = true;
    public static boolean enableMilestones = true;
    public static boolean enableSeasonHooks = true;
    public static boolean enableAbsorptionModifiers = true;
    public static boolean enableDebugLogging = false;

    private ModuleCache() {}

    public static void refresh() {
        NourishedConfig config = NourishedConfig.get();
        enableDecay = config.isModuleEnabled("enableDecay");
        enableNutritionEating = config.isModuleEnabled("enableNutritionEating");
        enableBlockHeavyMeals = config.isModuleEnabled("blockHeavyMeals");
        heavyMealNutritionThreshold = config.heavyMealNutritionThreshold();
        enableBlockLightFood = config.isModuleEnabled("blockLightFood");
        enableEffects = config.isModuleEnabled("enableEffects");
        enableHUD = config.isModuleEnabled("enableHUD");
        enableToasts = config.isModuleEnabled("enableToasts");
        enableFoodTooltips = config.isModuleEnabled("enableFoodTooltips");
        enableCalorieTracking = config.isModuleEnabled("enableCalorieTracking");
        enableDietScreen = config.isModuleEnabled("enableDietScreen");
        enableCriticalToasts = config.isModuleEnabled("enableCriticalToasts");
        enableSleepBonus = config.isModuleEnabled("enableSleepBonus");
        enableRawFoodPenalty = config.isModuleEnabled("enableRawFoodPenalty");
        enablePSStaminaUsage = config.isModuleEnabled("enablePSStaminaUsage");
        enablePSPenaltyDecay = config.isModuleEnabled("enablePSPenaltyDecay");
        enablePSExhaustionDuration = config.isModuleEnabled("enablePSExhaustionDuration");
        enableSOLDiversityHealth = config.isModuleEnabled("enableSOLDiversityHealth");
        enableSOLDiversityPenalty = config.isModuleEnabled("enableSOLDiversityPenalty");
        enableLSOThermalResistance = config.isModuleEnabled("enableLSOThermalResistance");
        enableLSOBrokenHeartResilience = config.isModuleEnabled("enableLSOBrokenHeartResilience");
        enableLSOThirstSaturation = config.isModuleEnabled("enableLSOThirstSaturation");
        enableSynergies = config.isModuleEnabled("enableSynergies");
        enableMilestones = config.isModuleEnabled("enableMilestones");
        enableSeasonHooks = config.isModuleEnabled("enableSeasonHooks");
        enableAbsorptionModifiers = config.isModuleEnabled("enableAbsorptionModifiers");
        enableDebugLogging = config.isModuleEnabled("enableDebugLogging");
    }
}
