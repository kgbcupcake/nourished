package dev.maire.nourished.modules.RawFood.rawInfo;

import dev.marie.MariesLib.api.ApiStatus;

import java.util.Map;

/**
 * Holds resistance configuration for one raw food severity tier.
 *
 * @param nutrientWeights    nutrient key to resistance contribution weight
 * @param resistanceThreshold minimum nutrient value required to contribute resistance
 * @param maxResistance      hard cap on total resistance [0.0, 1.0]
 */
@ApiStatus.Internal
public record RawFoodResistanceConfig(
        Map<String, Float> nutrientWeights,
        float resistanceThreshold,
        float maxResistance
) {
    public static final RawFoodResistanceConfig EMPTY = new RawFoodResistanceConfig(Map.of(), 1.0f, 0.0f);
}
