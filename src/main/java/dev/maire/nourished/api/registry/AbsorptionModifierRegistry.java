package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NutrientAbsorptionModifier;
import dev.maire.nourished.core.registry.ListRegistry;

import java.util.Comparator;
import java.util.List;

/**
 * Internal storage for nutrient absorption modifiers registered via the public API.
 */
@ApiStatus.Internal
public final class AbsorptionModifierRegistry {

    private static final ListRegistry<NutrientAbsorptionModifier> REGISTRY = new ListRegistry<>(
            "AbsorptionModifierRegistry",
            Comparator.comparingInt(NutrientAbsorptionModifier::getPriority)
    );

    private AbsorptionModifierRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    /**
     * Registers an absorption modifier. Priority ordering is applied when the registry is frozen.
     *
     * @param modifier the absorption modifier to register
     * @throws IllegalArgumentException if {@code modifier} is null
     */
    public static void register(NutrientAbsorptionModifier modifier) {
        REGISTRY.register(modifier);
    }

    /**
     * Returns all registered absorption modifiers sorted by priority.
     *
     * @return an unmodifiable list of absorption modifiers
     */
    public static List<NutrientAbsorptionModifier> getAll() {
        return REGISTRY.values();
    }
}
