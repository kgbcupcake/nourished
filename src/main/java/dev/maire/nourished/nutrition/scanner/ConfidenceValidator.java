package dev.maire.nourished.nutrition.scanner;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Validates classification confidence using spread-based analysis.
 *
 * <p>Key principle: Never use raw threshold. Always use spread.
 * A score of 8 vs 7 is uncertain. A score of 8 vs 1 is confident.</p>
 */
public final class ConfidenceValidator {

    private final float spreadThreshold;

    /**
     * @param spreadThreshold Minimum spread between dominant and secondary scores for confident classification
     */
    public ConfidenceValidator(float spreadThreshold) {
        this.spreadThreshold = spreadThreshold;
    }

    /**
     * Validate and build a classification result with confidence metrics.
     *
     * @param itemId The item's registry ID
     * @param scores Map of nutrient key to total score
     * @param signals List of signals that contributed to classification
     * @return Complete classification result with confidence data
     */
    public ClassificationResult validate(
            ResourceLocation itemId,
            Map<String, Float> scores,
            List<ClassificationSignal> signals
    ) {
        List<Map.Entry<String, Float>> sorted = scores.entrySet().stream()
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .toList();

        if (sorted.isEmpty()) {
            return ClassificationResult.empty(itemId, "grains");
        }

        String dominant = sorted.get(0).getKey();
        float dominantScore = sorted.get(0).getValue();

        String secondary = null;
        float secondaryScore = 0f;

        if (sorted.size() > 1) {
            secondary = sorted.get(1).getKey();
            secondaryScore = sorted.get(1).getValue();
        }

        float spread = dominantScore - secondaryScore;
        boolean uncertain = spread < spreadThreshold;

        return new ClassificationResult(
                itemId,
                scores,
                dominant,
                secondary,
                spread,
                signals,
                uncertain
        );
    }

    /**
     * Check if a pre-built result would be considered uncertain.
     */
    public boolean isUncertain(ClassificationResult result) {
        return result.confidenceSpread() < spreadThreshold;
    }

    /**
     * Calculate confidence percentage based on spread relative to threshold.
     * Returns 0-100 where 100 means spread >= 2x threshold.
     */
    public int confidencePercentage(ClassificationResult result) {
        if (result.confidenceSpread() <= 0) {
            return 0;
        }
        float ratio = result.confidenceSpread() / spreadThreshold;
        return Math.min(100, Math.round(ratio * 50));
    }

    /**
     * Get the configured spread threshold.
     */
    public float getSpreadThreshold() {
        return spreadThreshold;
    }

    /**
     * Create a validator with the default spread threshold (3.0).
     */
    public static ConfidenceValidator withDefaultThreshold() {
        return new ConfidenceValidator(3.0f);
    }
}
