package dev.maire.nourished.client.screen.diet.classic;

import dev.marie.framework.client.config.render.MarieValueColors;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.config.NourishedConfig;

/**
 * Color/threshold logic for the classic Diet Screen — extracted verbatim from the historical
 * monolithic {@code ClassicDietScreen}; no logic changes, only relocated.
 */
final class ClassicDietColorLogic {

    private ClassicDietColorLogic() {}

    static String getBalanceKey(TrackingData data) {
        NourishedConfig config = NourishedConfig.get();
        float critical = (float) config.criticalThreshold();
        float excessThreshold = (float) config.excessThreshold();
        boolean low = data.values.values().stream().anyMatch(v -> v < critical);
        boolean excess = data.values.values().stream().anyMatch(v -> v > excessThreshold);
        if (low)    return "low";
        if (excess) return "excess";
        return "balanced";
    }

    static int barColor(String key, float v) {
        NourishedConfig config = NourishedConfig.get();
        float critical = (float) config.criticalThresholdFor(key);
        float low = (float) config.lowThreshold();
        if (v < critical) return ClassicDietDrawHelpers.COL_RED;
        if (v < low) return ClassicDietDrawHelpers.COL_ORANGE;
        return nutrientBaseColor(key);
    }

    static int nutrientBaseColor(String key) {
        return MarieValueColors.baseColorArgb(key);
    }

    static int balanceColor(String key) {
        return switch (key) {
            case "balanced" -> ClassicDietDrawHelpers.COL_GREEN;
            case "low"      -> ClassicDietDrawHelpers.COL_ORANGE;
            case "excess"   -> ClassicDietDrawHelpers.COL_RED;
            default         -> ClassicDietDrawHelpers.COL_WHITE;
        };
    }
}
