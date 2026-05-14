package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.config.NourishedConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shared numeric helpers used by multiple resolution stages.
 * Package-private — not part of the public API surface.
 */
final class StageMath {

    private StageMath() {}

    static float computeSpread(Map<String, Float> scores) {
        float first = Float.NEGATIVE_INFINITY;
        float second = Float.NEGATIVE_INFINITY;
        for (float v : scores.values()) {
            if (v > first) {
                second = first;
                first = v;
            } else if (v > second) {
                second = v;
            }
        }
        if (first == Float.NEGATIVE_INFINITY) return 0f;
        if (second == Float.NEGATIVE_INFINITY) return first;
        return first - second;
    }

    /**
     * Normalizes raw scores into the bar map shape expected downstream:
     * confident dominant (spread >= threshold) produces a single 1.0 entry,
     * otherwise normalizes all positive mass. Filters to valid nutrient keys.
     */
    static Map<String, Float> normalizeToBarMap(Map<String, Float> raw, Set<String> validKeys) {
        Map<String, Float> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : raw.entrySet()) {
            if (validKeys.contains(e.getKey()) && e.getValue() > 0f) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        if (filtered.isEmpty()) return Map.of();

        float max = Float.NEGATIVE_INFINITY;
        String dominant = null;
        float second = Float.NEGATIVE_INFINITY;
        for (Map.Entry<String, Float> e : filtered.entrySet()) {
            if (e.getValue() > max) {
                second = max;
                max = e.getValue();
                dominant = e.getKey();
            } else if (e.getValue() > second) {
                second = e.getValue();
            }
        }

        float spread = (second == Float.NEGATIVE_INFINITY) ? max : max - second;
        float threshold = scannerConfidenceSpreadThreshold();

        if (spread >= threshold && dominant != null) {
            return Map.of(dominant, 1.0f);
        }

        float sum = 0f;
        for (float v : filtered.values()) sum += v;
        if (sum <= 1e-5f) return Map.of();

        Map<String, Float> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : filtered.entrySet()) {
            normalized.put(e.getKey(), e.getValue() / sum);
        }
        return normalized;
    }

    static float scannerConfidenceSpreadThreshold() {
        try {
            return (float) NourishedConfig.get().scannerConfidenceSpreadThreshold();
        } catch (IllegalStateException ignored) {
            return 0f;
        }
    }
}
