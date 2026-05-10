package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.FoodSynergyDefinition;
import dev.maire.nourished.api.NutrientSynergyDefinition;
import dev.maire.nourished.core.registry.ListRegistry;

import java.util.List;

/**
 * Internal storage for nutrient synergy and food synergy definitions
 * registered via the public API.
 */
@ApiStatus.Internal
public final class SynergyRegistry {

    private static final ListRegistry<NutrientSynergyDefinition> NUTRIENT_SYNERGIES =
            new ListRegistry<>("SynergyRegistry.nutrientSynergies", null);
    private static final ListRegistry<FoodSynergyDefinition> FOOD_SYNERGIES =
            new ListRegistry<>("SynergyRegistry.foodSynergies", null);

    private SynergyRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        NUTRIENT_SYNERGIES.freeze();
        FOOD_SYNERGIES.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        NUTRIENT_SYNERGIES.reset();
        FOOD_SYNERGIES.reset();
    }

    /**
     * Registers a nutrient synergy definition.
     *
     * @param definition the nutrient synergy to register
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void registerNutrientSynergy(NutrientSynergyDefinition definition) {
        NUTRIENT_SYNERGIES.register(definition);
    }

    /**
     * Registers a food synergy definition.
     *
     * @param definition the food synergy to register
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void registerFoodSynergy(FoodSynergyDefinition definition) {
        FOOD_SYNERGIES.register(definition);
    }

    /**
     * Returns all registered nutrient synergies.
     *
     * @return an unmodifiable list of nutrient synergy definitions
     */
    public static List<NutrientSynergyDefinition> getNutrientSynergies() {
        return NUTRIENT_SYNERGIES.values();
    }

    /**
     * Returns all registered food synergies.
     *
     * @return an unmodifiable list of food synergy definitions
     */
    public static List<FoodSynergyDefinition> getFoodSynergies() {
        return FOOD_SYNERGIES.values();
    }
}
