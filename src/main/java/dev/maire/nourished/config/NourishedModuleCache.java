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

    private NourishedModuleCache() {}

    public static void refresh(NourishedConfig c) {
        heavySourcePropertyThreshold = c.heavySourcePropertyThreshold();
        enableRawSourcePenalty = c.isModuleEnabled("enableRawFoodPenalty");
        enableGutHealth = c.isModuleEnabled("enableGutHealth");
    }
}
