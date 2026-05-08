package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;

/**
 * Defines a compatibility entry for mapping another mod's food items to
 * Nourished nutrient keys.
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link NourishedAPI#registerCompatEntry(CompatDefinition)}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * CompatDefinition farmersDelight = CompatDefinition.builder("farmersdelight")
 *     .category(CompatCategory.FOOD_MOD)
 *     .addFoodTagMapping(ResourceLocation.parse("farmersdelight:fried_rice"), "carbs")
 *     .addFoodTagMapping(ResourceLocation.parse("farmersdelight:steak"), "protein")
 *     .build();
 * }</pre>
 */
@ApiStatus.Stable
public final class CompatDefinition {

    /**
     * Categorizes the type of mod being integrated with.
     */
    public enum CompatCategory {
        /** A mod that primarily adds new food items. */
        FOOD_MOD,
        /** A mod focused on farming and crop mechanics. */
        FARMING_MOD,
        /** A mod that overhauls survival mechanics broadly. */
        SURVIVAL_OVERHAUL
    }

    private final String modId;
    private final CompatCategory category;
    private final Map<ResourceLocation, String> foodTagMappings;

    private CompatDefinition(String modId, CompatCategory category, Map<ResourceLocation, String> foodTagMappings) {
        this.modId = modId;
        this.category = category;
        this.foodTagMappings = Collections.unmodifiableMap(foodTagMappings);
    }

    /**
     * Creates a new builder for a compatibility definition targeting the given mod.
     *
     * @param modId the mod identifier (e.g. "farmersdelight")
     * @return a new {@link Builder} instance
     */
    public static Builder builder(String modId) {
        return new Builder(modId);
    }

    /**
     * Returns the target mod's identifier.
     *
     * @return the mod id string
     */
    public String getModId() {
        return modId;
    }

    /**
     * Returns the category classification of the target mod.
     *
     * @return the {@link CompatCategory} enum value
     */
    public CompatCategory getCategory() {
        return category;
    }

    /**
     * Returns an unmodifiable map of food item identifiers to their assigned
     * nutrient keys.
     *
     * @return the food-to-nutrient mapping
     */
    public Map<ResourceLocation, String> getFoodTagMappings() {
        return foodTagMappings;
    }

    /**
     * Builder for constructing {@link CompatDefinition} instances.
     */
    public static final class Builder {

        private final String modId;
        private CompatCategory category = CompatCategory.FOOD_MOD;
        private final Map<ResourceLocation, String> foodTagMappings = new java.util.HashMap<>();

        private Builder(String modId) {
            this.modId = modId;
        }

        /**
         * Sets the category classification for the target mod.
         *
         * @param category the {@link CompatCategory} value
         * @return this builder for chaining
         */
        public Builder category(CompatCategory category) {
            this.category = category;
            return this;
        }

        /**
         * Adds a food item to nutrient key mapping.
         *
         * @param foodId      the registry identifier of the food item
         * @param nutrientKey the nutrient key to assign (e.g. "protein")
         * @return this builder for chaining
         */
        public Builder addFoodTagMapping(ResourceLocation foodId, String nutrientKey) {
            this.foodTagMappings.put(foodId, nutrientKey);
            return this;
        }

        /**
         * Adds all food-to-nutrient mappings from the provided map.
         *
         * @param mappings a map of food identifiers to nutrient keys
         * @return this builder for chaining
         */
        public Builder addAllFoodTagMappings(Map<ResourceLocation, String> mappings) {
            this.foodTagMappings.putAll(mappings);
            return this;
        }

        /**
         * Builds and returns the immutable {@link CompatDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        public CompatDefinition build() {
            if (modId == null || modId.trim().isEmpty()) {
                throw new IllegalStateException("CompatDefinition requires a non-empty modId.");
            }

            CompatCategory resolvedCategory = category != null ? category : CompatCategory.FOOD_MOD;
            Map<ResourceLocation, String> resolvedFoodTagMappings =
                    foodTagMappings != null ? foodTagMappings : Collections.emptyMap();

            return new CompatDefinition(modId, resolvedCategory, resolvedFoodTagMappings);
        }
    }
}
