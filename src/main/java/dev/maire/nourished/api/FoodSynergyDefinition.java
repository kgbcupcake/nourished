package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Defines a meal combo synergy: eating food A within a time window of food B
 * grants a bonus nutrient burst. Models real-world meal pairing concepts
 * (e.g. iron absorption boosted by vitamin C sources).
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link NourishedAPI#registerFoodSynergy(FoodSynergyDefinition)}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * FoodSynergyDefinition combo = FoodSynergyDefinition.builder("steak_potato")
 *     .foodA(ResourceLocation.parse("minecraft:cooked_beef"))
 *     .foodB(ResourceLocation.parse("minecraft:baked_potato"))
 *     .timeWindowTicks(100)  // 5 seconds
 *     .bonusNutrientKey("protein")
 *     .bonusAmount(0.15f)
 *     .build();
 * }</pre>
 */
@ApiStatus.Experimental
public final class FoodSynergyDefinition {

    private final String id;
    private final ResourceLocation foodA;
    private final ResourceLocation foodB;
    private final int timeWindowTicks;
    private final String bonusNutrientKey;
    private final float bonusAmount;

    private FoodSynergyDefinition(
            String id,
            ResourceLocation foodA,
            ResourceLocation foodB,
            int timeWindowTicks,
            String bonusNutrientKey,
            float bonusAmount
    ) {
        this.id = id;
        this.foodA = foodA;
        this.foodB = foodB;
        this.timeWindowTicks = timeWindowTicks;
        this.bonusNutrientKey = bonusNutrientKey;
        this.bonusAmount = bonusAmount;
    }

    /**
     * Creates a new builder for a food synergy definition.
     *
     * @param id the unique identifier for this food synergy (e.g. "steak_potato")
     * @return a new {@link Builder} instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique identifier for this food synergy.
     *
     * @return the synergy id string
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the first food item in the synergy pair.
     *
     * @return the {@link ResourceLocation} of food A
     */
    public ResourceLocation getFoodA() {
        return foodA;
    }

    /**
     * Returns the second food item in the synergy pair.
     *
     * @return the {@link ResourceLocation} of food B
     */
    public ResourceLocation getFoodB() {
        return foodB;
    }

    /**
     * Returns the time window in game ticks within which both foods must
     * be consumed for the synergy to activate.
     *
     * @return the time window in ticks (20 ticks = 1 second)
     */
    public int getTimeWindowTicks() {
        return timeWindowTicks;
    }

    /**
     * Returns the nutrient key that receives the bonus burst when
     * the synergy activates.
     *
     * @return the bonus nutrient key (e.g. "protein")
     */
    public String getBonusNutrientKey() {
        return bonusNutrientKey;
    }

    /**
     * Returns the bonus nutrient amount added when the synergy activates.
     *
     * @return the bonus amount as a float
     */
    public float getBonusAmount() {
        return bonusAmount;
    }

    /**
     * Builder for constructing {@link FoodSynergyDefinition} instances.
     */
    public static final class Builder {

        private final String id;
        private ResourceLocation foodA;
        private ResourceLocation foodB;
        private int timeWindowTicks = 100;
        private String bonusNutrientKey;
        private float bonusAmount;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the first food item in the synergy pair.
         *
         * @param foodA the {@link ResourceLocation} of the first food
         * @return this builder for chaining
         */
        public Builder foodA(ResourceLocation foodA) {
            this.foodA = foodA;
            return this;
        }

        /**
         * Sets the second food item in the synergy pair.
         *
         * @param foodB the {@link ResourceLocation} of the second food
         * @return this builder for chaining
         */
        public Builder foodB(ResourceLocation foodB) {
            this.foodB = foodB;
            return this;
        }

        /**
         * Sets the time window within which both foods must be consumed
         * for the synergy to activate.
         *
         * @param ticks the time window in game ticks (20 ticks = 1 second)
         * @return this builder for chaining
         */
        public Builder timeWindowTicks(int ticks) {
            this.timeWindowTicks = ticks;
            return this;
        }

        /**
         * Sets the nutrient key that receives the bonus when activated.
         *
         * @param nutrientKey the bonus nutrient key (e.g. "protein")
         * @return this builder for chaining
         */
        public Builder bonusNutrientKey(String nutrientKey) {
            this.bonusNutrientKey = nutrientKey;
            return this;
        }

        /**
         * Sets the bonus nutrient amount granted on synergy activation.
         *
         * @param amount the bonus amount
         * @return this builder for chaining
         */
        public Builder bonusAmount(float amount) {
            this.bonusAmount = amount;
            return this;
        }

        /**
         * Builds and returns the immutable {@link FoodSynergyDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        public FoodSynergyDefinition build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (foodA == null) {
                throw new IllegalStateException("foodA is required");
            }
            if (foodB == null) {
                throw new IllegalStateException("foodB is required");
            }
            if (bonusNutrientKey == null) {
                throw new IllegalStateException("bonusNutrientKey is required");
            }
            return new FoodSynergyDefinition(id, foodA, foodB, timeWindowTicks, bonusNutrientKey, bonusAmount);
        }
    }
}
