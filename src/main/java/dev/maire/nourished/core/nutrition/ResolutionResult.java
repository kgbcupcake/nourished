package dev.maire.nourished.core.nutrition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable result of a single runtime food resolution attempt.
 */
public record ResolutionResult(
        Map<String, Float> nutrients,
        float confidence,
        ResolutionStage stage,
        String debugReason
) {
    public ResolutionResult {
        nutrients = Map.copyOf(nutrients);
    }

    public Map<String, Float> toNutrientMap() {
        return new LinkedHashMap<>(nutrients);
    }
}
