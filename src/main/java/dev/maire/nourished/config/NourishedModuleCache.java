package dev.maire.nourished.config;

/**
 * Cached module toggle values for hot gameplay paths.
 * Mirrors NourishedConfig values for tick-safe access.
 * Refresh by calling refresh(NourishedConfig) after config load/reload.
 */
public final class NourishedModuleCache {

    public static int heavySourcePropertyThreshold = 6;
    public static boolean enableRawSourcePenalty = true;
    public static boolean enableGutHealth = true;
    public static boolean enableStamina = false;
    public static boolean enablePSStaminaUsage = true;
    public static boolean enablePSPenaltyDecay = true;
    public static boolean enablePSExhaustionDuration = true;
    public static boolean enableSOLDiversityHealth = false;
    public static boolean enableSOLDiversityPenalty = true;
    public static boolean enableLSOThermalResistance = true;
    public static boolean enableLSOBrokenHeartResilience = true;
    public static boolean enableLSOThirstSaturation = true;

    private NourishedModuleCache() {}

    public static void refresh(NourishedConfig c) {
        heavySourcePropertyThreshold = c.heavySourcePropertyThreshold();
        enableRawSourcePenalty = c.isModuleEnabled("enableRawFoodPenalty");
        enableGutHealth = c.isModuleEnabled("enableGutHealth");
        enableStamina = c.isModuleEnabled("enableStamina");
        enablePSStaminaUsage = c.isModuleEnabled("enablePSStaminaUsage");
        enablePSPenaltyDecay = c.isModuleEnabled("enablePSPenaltyDecay");
        enablePSExhaustionDuration = c.isModuleEnabled("enablePSExhaustionDuration");
        enableSOLDiversityHealth = c.isModuleEnabled("enableSOLDiversityHealth");
        enableSOLDiversityPenalty = c.isModuleEnabled("enableSOLDiversityPenalty");
        enableLSOThermalResistance = c.isModuleEnabled("enableLSOThermalResistance");
        enableLSOBrokenHeartResilience = c.isModuleEnabled("enableLSOBrokenHeartResilience");
        enableLSOThirstSaturation = c.isModuleEnabled("enableLSOThirstSaturation");
    }
}
