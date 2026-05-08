package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Read-only view into a player's food memory, exposing recent consumption
 * history without allowing mutation.
 *
 * <p>Use this to query what a player has eaten recently, check for dietary
 * variety, or implement diminishing-returns mechanics.</p>
 */
@ApiStatus.Stable
public interface FoodMemoryView {

    /**
     * Returns an ordered list of recently consumed food identifiers,
     * from most recent to oldest.
     *
     * @return an unmodifiable list of food {@link ResourceLocation} identifiers
     */
    List<ResourceLocation> getRecentFoods();

    /**
     * Checks whether the player has consumed the specified food within
     * the memory window (determined by configuration).
     *
     * @param foodId the registry identifier of the food item to check
     * @return {@code true} if the food exists in the player's recent memory
     */
    boolean hasEatenRecently(ResourceLocation foodId);

    /**
     * Returns the time in game ticks since the player last ate the specified food.
     *
     * @param foodId the registry identifier of the food item to query
     * @return the elapsed ticks since last consumption, or {@code -1} if never eaten
     *         within the memory window
     */
    long getTimeSinceEaten(ResourceLocation foodId);
}
