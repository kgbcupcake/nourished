package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.NutrientAbsorptionModifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal storage for nutrient absorption modifiers registered via the public API.
 */
public final class AbsorptionModifierRegistry {

    private static final List<NutrientAbsorptionModifier> MODIFIERS = new ArrayList<>();

    private AbsorptionModifierRegistry() {}

    /**
     * Registers an absorption modifier. Modifiers are stored in registration order
     * and sorted by priority when applied.
     *
     * @param modifier the absorption modifier to register
     */
    public static void register(NutrientAbsorptionModifier modifier) {
        MODIFIERS.add(modifier);
        MODIFIERS.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
    }

    /**
     * Returns all registered absorption modifiers sorted by priority.
     *
     * @return an unmodifiable list of absorption modifiers
     */
    public static List<NutrientAbsorptionModifier> getAll() {
        return Collections.unmodifiableList(MODIFIERS);
    }
}
