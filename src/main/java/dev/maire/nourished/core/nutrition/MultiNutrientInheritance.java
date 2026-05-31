package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.config.NourishedConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared multi-nutrient recipe inheritance filtering used by the runtime resolver
 * and the food scanner.
 */
public final class MultiNutrientInheritance {

    private static final float DEFAULT_THRESHOLD = 0.20f;

    private MultiNutrientInheritance() {}

    /**
     * Keeps nutrients whose aggregated share meets {@code threshold}; re-normalizes when multiple qualify.
     */
    public static Map<String, Float> filterQualifyingNutrients(Map<String, Float> aggregated, float threshold) {
        float total = 0f;
        for (float v : aggregated.values()) {
            total += v;
        }
        if (total <= 0f) {
            return Map.of();
        }

        Map<String, Float> qualifying = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : aggregated.entrySet()) {
            if (e.getValue() / total >= threshold) {
                qualifying.put(e.getKey(), e.getValue());
            }
        }
        if (qualifying.isEmpty()) {
            return Map.of();
        }
        if (qualifying.size() == 1) {
            return qualifying;
        }

        float qualTotal = 0f;
        for (float v : qualifying.values()) {
            qualTotal += v;
        }
        Map<String, Float> proportional = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : qualifying.entrySet()) {
            proportional.put(e.getKey(), e.getValue() / qualTotal);
        }
        return proportional;
    }

    public static float threshold() {
        try {
            return NourishedConfig.get().multiNutrientInheritanceThreshold();
        } catch (IllegalStateException ignored) {
            return DEFAULT_THRESHOLD;
        }
    }
}
