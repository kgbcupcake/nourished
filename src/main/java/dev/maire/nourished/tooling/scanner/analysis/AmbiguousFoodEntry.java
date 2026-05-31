package dev.maire.nourished.tooling.scanner.analysis;

import dev.maire.nourished.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * A food item whose dominant and second-highest nutrient scores are too close
 * to classify confidently. Flagged for manual review.
 *
 * @param itemId The item's registry ID
 * @param scores All nutrient scores from classification
 * @param spread Difference between dominant and second-highest scores
 */
@ApiStatus.Internal
public record AmbiguousFoodEntry(
        ResourceLocation itemId,
        Map<String, Float> scores,
        float spread
) {
    public AmbiguousFoodEntry {
        scores = Map.copyOf(scores);
    }
}
