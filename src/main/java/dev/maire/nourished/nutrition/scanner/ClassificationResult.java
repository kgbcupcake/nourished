package dev.maire.nourished.nutrition.scanner;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Full classification result for a single food item.
 * Contains the weighted nutrient scores, dominant/secondary categories,
 * confidence spread, and full signal trace.
 *
 * @param itemId The item's registry ID
 * @param scores Map of nutrient key to total weighted score
 * @param dominant The highest-scoring nutrient category
 * @param secondary The second-highest-scoring nutrient category (may be null)
 * @param confidenceSpread The difference between dominant and secondary scores
 * @param signals List of all signals that contributed to this classification
 * @param uncertain True if confidenceSpread is below the threshold
 */
public record ClassificationResult(
        ResourceLocation itemId,
        Map<String, Float> scores,
        String dominant,
        String secondary,
        float confidenceSpread,
        List<ClassificationSignal> signals,
        boolean uncertain
) {
    public ClassificationResult {
        scores = Map.copyOf(scores);
        signals = List.copyOf(signals);
    }

    /**
     * Returns the top N signals by total magnitude.
     */
    public List<ClassificationSignal> topSignals(int n) {
        return signals.stream()
                .sorted((a, b) -> Float.compare(b.totalMagnitude(), a.totalMagnitude()))
                .limit(n)
                .toList();
    }

    /**
     * Returns scores sorted by value descending.
     */
    public List<Map.Entry<String, Float>> sortedScores() {
        return scores.entrySet().stream()
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .toList();
    }

    /**
     * Creates an empty/default result for items that couldn't be classified.
     */
    public static ClassificationResult empty(ResourceLocation itemId, String fallbackNutrient) {
        return new ClassificationResult(
                itemId,
                Map.of(fallbackNutrient, 0f),
                fallbackNutrient,
                null,
                0f,
                Collections.emptyList(),
                true
        );
    }
}
