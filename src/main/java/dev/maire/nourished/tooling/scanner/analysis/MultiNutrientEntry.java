package dev.maire.nourished.tooling.scanner.analysis;

import dev.maire.nourished.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

/**
 * A food item that qualifies for a secondary nutrient tag recommendation.
 *
 * @param itemId  The item's registry ID
 * @param score   The secondary nutrient's weighted score
 * @param dominant The item's dominant nutrient category
 */
@ApiStatus.Internal
public record MultiNutrientEntry(
        ResourceLocation itemId,
        float score,
        String dominant
) {}
