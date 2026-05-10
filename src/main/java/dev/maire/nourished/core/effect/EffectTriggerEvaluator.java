package dev.maire.nourished.effect;

import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.nutrition.NutrientRegistry;

final class EffectTriggerEvaluator {

    private EffectTriggerEvaluator() {}

    static boolean evaluate(EffectRegistry.EffectDef def, DietData data) {
        return switch (def.trigger()) {
            case "below" -> data.nutrients.getOrDefault(def.nutrient(), 0f) < def.threshold();
            case "above" -> data.nutrients.getOrDefault(def.nutrient(), 0f) > def.threshold();
            case "all_above" -> NutrientRegistry.getKeys().stream()
                    .allMatch(k -> data.nutrients.getOrDefault(k, 0f) > def.threshold());
            case "any_below" -> NutrientRegistry.getKeys().stream()
                    .anyMatch(k -> data.nutrients.getOrDefault(k, 0f) < def.threshold());
            case "between" -> {
                float v = data.nutrients.getOrDefault(def.nutrient(), 0f);
                yield v >= def.threshold() && v <= def.thresholdMax();
            }
            default -> false;
        };
    }
}
