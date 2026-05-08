package dev.maire.nourished.api;

import java.util.Map;

/**
 * Immutable snapshot of a player's current nutrition state.
 *
 * <p>This record combines calorie totals, nutrient levels, and food memory
 * into a single API value object suitable for read-only consumption.</p>
 *
 * @param calories   the player's current calorie value
 * @param nutrients  unmodifiable nutrient map keyed by nutrient id
 * @param foodMemory read-only view of the player's food memory state
 */
@ApiStatus.Stable
public record NourishedPlayerData(float calories, Map<String, Float> nutrients, FoodMemoryView foodMemory) {
}
