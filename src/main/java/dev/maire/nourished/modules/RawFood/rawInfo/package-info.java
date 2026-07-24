/**
 * Raw food classification, cookedness resolution, cooked-version nutrition lookup,
 * and resistance calculation.
 *
 * <ul>
 *   <li>{@link RawFoodClassifier} — determines raw food severity for an item</li>
 *   <li>{@link CookednessResolver} — resolves cookedness float (0.0 raw to 1.0 cooked)</li>
 *   <li>{@link CookedVersionResolver} — resolves nutrient bars for the cooked version of a raw item</li>
 *   <li>{@link RawFoodResistanceConfig} — holds resistance configuration for one severity tier</li>
 *   <li>{@link RawFoodResistanceResolver} — resolves resistance float based on player nutrient levels</li>
 * </ul>
 *
 * <p>Classification and cookedness results are cached after first resolution.
 * The hot path (player eating) performs only cache lookups for those values.</p>
 *
 * <p>Resistance is NOT cached — it reads current nutrient bar values on every call
 * since those values change constantly. The calculation is a simple map lookup
 * and float multiply, so performance is acceptable on the hot path.</p>
 */
@ApiStatus.Internal
package dev.maire.nourished.modules.RawFood.rawInfo;

import dev.marie.framework.api.ApiStatus;
