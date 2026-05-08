package dev.maire.nourished.api;

import net.minecraft.world.entity.player.Player;

/**
 * Modifier interface that adjusts how much of a nutrient a player actually
 * absorbs from a food item, based on their current state.
 *
 * <p>This enables mechanics like reduced absorption when sick, boosted
 * absorption when well-fed, or diminished gains when starving. No other
 * nutrition mod offers per-state absorption modification.</p>
 *
 * <p>Register implementations via
 * {@link NourishedAPI#registerAbsorptionModifier(NutrientAbsorptionModifier)}.</p>
 */
@ApiStatus.Experimental
public interface NutrientAbsorptionModifier {

    /**
     * Returns a unique identifier for this modifier, used for ordering and debugging.
     *
     * @return a non-null, unique string identifier (e.g. "mymod:illness_penalty")
     */
    String getModifierId();

    /**
     * Calculates the absorption multiplier for the given player and nutrient.
     *
     * <p>The returned value multiplies the base nutrient gain. For example:</p>
     * <ul>
     *   <li>{@code 1.0} — no change (default absorption)</li>
     *   <li>{@code 0.5} — player absorbs only half the nutrient</li>
     *   <li>{@code 1.5} — player absorbs 50% more of the nutrient</li>
     *   <li>{@code 0.0} — nutrient is completely blocked</li>
     * </ul>
     *
     * @param player      the player consuming the food
     * @param nutrientKey the internal key of the nutrient being absorbed (e.g. "protein")
     * @param baseAmount  the unmodified nutrient amount from the food item
     * @return the multiplicative absorption factor; must be non-negative
     */
    float getAbsorptionMultiplier(Player player, String nutrientKey, float baseAmount);

    /**
     * Returns the priority of this modifier for ordering purposes.
     * Lower values are applied first. Default priority is {@code 0}.
     *
     * @return the priority value for this modifier
     */
    default int getPriority() {
        return 0;
    }
}
