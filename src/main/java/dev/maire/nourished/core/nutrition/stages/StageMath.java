package dev.maire.nourished.core.nutrition.stages;

import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.nutrition.ResolutionResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shared numeric helpers used by multiple resolution stages.
 * Package-private — not part of the public API surface.
 */
final class StageMath {

    private StageMath() {}

    record NormalizationOutcome(
            Map<String, Float> normalized,
            Map<String, String> rejectedSignals
    ) {}

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
        return normalizeWithRejections(raw, validKeys).normalized();
    }

    /**
     * Full normalization producing both the bar map and a rejection reason
     * for every valid nutrient key that did not make it into the final map.
     */
    static NormalizationOutcome normalizeWithRejections(Map<String, Float> raw, Set<String> validKeys) {
        Map<String, Float> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : raw.entrySet()) {
            if (validKeys.contains(e.getKey()) && e.getValue() > 0f) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        if (filtered.isEmpty()) {
            Map<String, String> rejected = new LinkedHashMap<>();
            for (String key : validKeys) {
                rejected.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
            }
            return new NormalizationOutcome(Map.of(), rejected);
        }

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
            Map<String, String> rejected = new LinkedHashMap<>();
            for (String key : filtered.keySet()) {
                if (!key.equals(dominant)) {
                    rejected.put(key, ResolutionResult.REJECT_LOW_CONFIDENCE);
                }
            }
            for (String key : validKeys) {
                if (!filtered.containsKey(key)) {
                    rejected.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
                }
            }
            return new NormalizationOutcome(Map.of(dominant, 1.0f), rejected);
        }

        float sum = 0f;
        for (float v : filtered.values()) sum += v;
        if (sum <= 1e-5f) {
            Map<String, String> rejected = new LinkedHashMap<>();
            for (String key : validKeys) {
                rejected.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
            }
            return new NormalizationOutcome(Map.of(), rejected);
        }

        Map<String, Float> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Float> e : filtered.entrySet()) {
            normalized.put(e.getKey(), e.getValue() / sum);
        }
        Map<String, String> rejected = new LinkedHashMap<>();
        for (String key : validKeys) {
            if (!filtered.containsKey(key)) {
                rejected.put(key, ResolutionResult.REJECT_NO_MATCHING_KEYWORDS);
            }
        }
        return new NormalizationOutcome(normalized, rejected);
    }

    static float scannerConfidenceSpreadThreshold() {
        try {
            return (float) NourishedConfig.get().scannerConfidenceSpreadThreshold();
        } catch (IllegalStateException ignored) {
            return 0f;
        }
    }
}
