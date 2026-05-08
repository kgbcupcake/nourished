package dev.maire.nourished.api;

/**
 * Hook interface for integrating with Serene Seasons (or similar season mods),
 * allowing nutrient decay rates and food availability to vary per season.
 *
 * <p>Implement this interface and register it via
 * {@link NourishedAPI#registerSeasonHook(NourishedSeasonHook)} to provide
 * seasonal modifiers to the nutrition system.</p>
 */
@ApiStatus.Experimental
public interface NourishedSeasonHook {

    /**
     * Represents the four canonical seasons used by the hook system.
     */
    enum Season {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }

    /**
     * Returns a decay rate modifier for the specified nutrient during the given season.
     *
     * <p>A value of {@code 1.0} means no change, values greater than {@code 1.0}
     * accelerate decay, and values less than {@code 1.0} slow it down.</p>
     *
     * @param nutrientKey the internal key identifying the nutrient (e.g. "protein")
     * @param season      the current season to calculate the modifier for
     * @return the multiplicative decay modifier; must be non-negative
     */
    float getSeasonalDecayModifier(String nutrientKey, Season season);

    /**
     * Returns a nutrient absorption modifier for the specified nutrient during
     * the given season, affecting how much nutrition is gained from food.
     *
     * <p>A value of {@code 1.0} means standard absorption. Values greater than
     * {@code 1.0} increase gains (e.g. seasonal produce in season), and values
     * less than {@code 1.0} reduce them.</p>
     *
     * @param nutrientKey the internal key identifying the nutrient
     * @param season      the current season to calculate the modifier for
     * @return the multiplicative absorption modifier; must be non-negative
     */
    float getSeasonalAbsorptionModifier(String nutrientKey, Season season);
}
