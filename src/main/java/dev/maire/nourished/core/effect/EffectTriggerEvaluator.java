package dev.maire.nourished.core.effect;

import dev.marie.MariesLib.tracking.TrackingData;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

final class EffectTriggerEvaluator {

    private EffectTriggerEvaluator() {}

    static boolean evaluate(EffectRegistry.EffectDef def, TrackingData data) {
        return switch (def.trigger()) {
            case "below" -> data.values.getOrDefault(def.nutrient(), 0f) < def.threshold();
            case "above" -> data.values.getOrDefault(def.nutrient(), 0f) > def.threshold();
            case "all_above" -> NutrientRegistry.getKeys().stream()
                    .allMatch(k -> data.values.getOrDefault(k, 0f) > def.threshold());
            case "any_below" -> NutrientRegistry.getKeys().stream()
                    .anyMatch(k -> data.values.getOrDefault(k, 0f) < def.threshold());
            case "between" -> {
                float v = data.values.getOrDefault(def.nutrient(), 0f);
                yield v >= def.threshold() && v <= def.thresholdMax();
            }
            default -> false;
        };
    }
}
