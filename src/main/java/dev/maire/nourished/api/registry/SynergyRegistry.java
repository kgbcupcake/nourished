package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.FoodSynergyDefinition;
import dev.maire.nourished.api.NutrientSynergyDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal storage for nutrient synergy and food synergy definitions
 * registered via the public API.
 */
@ApiStatus.Internal
public final class SynergyRegistry {

    private static final List<NutrientSynergyDefinition> NUTRIENT_SYNERGIES = new ArrayList<>();
    private static final List<FoodSynergyDefinition> FOOD_SYNERGIES = new ArrayList<>();

    private SynergyRegistry() {}

    /**
     * Registers a nutrient synergy definition.
     *
     * @param definition the nutrient synergy to register
     */
    public static void registerNutrientSynergy(NutrientSynergyDefinition definition) {
        NUTRIENT_SYNERGIES.add(definition);
    }

    /**
     * Registers a food synergy definition.
     *
     * @param definition the food synergy to register
     */
    public static void registerFoodSynergy(FoodSynergyDefinition definition) {
        FOOD_SYNERGIES.add(definition);
    }

    /**
     * Returns all registered nutrient synergies.
     *
     * @return an unmodifiable list of nutrient synergy definitions
     */
    public static List<NutrientSynergyDefinition> getNutrientSynergies() {
        return Collections.unmodifiableList(new ArrayList<>(NUTRIENT_SYNERGIES));
    }

    /**
     * Returns all registered food synergies.
     *
     * @return an unmodifiable list of food synergy definitions
     */
    public static List<FoodSynergyDefinition> getFoodSynergies() {
        return Collections.unmodifiableList(new ArrayList<>(FOOD_SYNERGIES));
    }
}
