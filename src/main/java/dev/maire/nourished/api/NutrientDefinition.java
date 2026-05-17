package dev.maire.nourished.api;

import javax.annotation.Nullable;

/**
 * Defines a custom nutrient that can be registered with the Nourished system.
 *
 * <p>Use the {@link Builder} to construct instances. Nutrients registered via this
 * definition will participate in decay, threshold checks, and HUD rendering.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * NutrientDefinition vitaminC = NutrientDefinition.builder("vitamin_c")
 *     .displayName("Vitamin C")
 *     .color(0xFFA500)
 *     .defaultDecayRate(0.002f)
 *     .criticalThreshold(0.1f)
 *     .lowThreshold(0.3f)
 *     .excessThreshold(0.9f)
 *     .build();
 * }</pre>
 */
@ApiStatus.Stable
public final class NutrientDefinition {

    private final String id;
    private final String displayName;
    private final int color;
    private final float defaultDecayRate;
    private final float criticalThreshold;
    private final float lowThreshold;
    private final float excessThreshold;
    @Nullable
    private final NutrientRenderer customRenderer;
    private final boolean beneficial;

    private NutrientDefinition(
            String id,
            String displayName,
            int color,
            float defaultDecayRate,
            float criticalThreshold,
            float lowThreshold,
            float excessThreshold,
            @Nullable NutrientRenderer customRenderer,
            boolean beneficial
    ) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.defaultDecayRate = defaultDecayRate;
        this.criticalThreshold = criticalThreshold;
        this.lowThreshold = lowThreshold;
        this.excessThreshold = excessThreshold;
        this.customRenderer = customRenderer;
        this.beneficial = beneficial;
    }

    /**
     * Creates a new builder for a nutrient with the given unique identifier.
     *
     * @param id the unique internal key for this nutrient (e.g. "vitamin_c")
     * @return a new {@link Builder} instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique internal identifier for this nutrient.
     *
     * @return the nutrient key string
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the human-readable display name for this nutrient.
     *
     * @return the display name string
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the ARGB color used for rendering this nutrient in the HUD.
     *
     * @return the color as a packed ARGB integer
     */
    public int getColor() {
        return color;
    }

    /**
     * Returns the default rate at which this nutrient decays per tick.
     *
     * @return the decay rate per tick (e.g. 0.001 means 0.1% per tick)
     */
    public float getDefaultDecayRate() {
        return defaultDecayRate;
    }

    /**
     * Returns the threshold below which the nutrient level is considered critical.
     *
     * @return the critical threshold as a normalized float (0.0 to 1.0)
     */
    public float getCriticalThreshold() {
        return criticalThreshold;
    }

    /**
     * Returns the threshold below which the nutrient level is considered low.
     *
     * @return the low threshold as a normalized float (0.0 to 1.0)
     */
    public float getLowThreshold() {
        return lowThreshold;
    }

    /**
     * Returns the threshold above which the nutrient level is considered excessive.
     *
     * @return the excess threshold as a normalized float (0.0 to 1.0)
     */
    public float getExcessThreshold() {
        return excessThreshold;
    }

    /**
     * Returns the optional custom renderer for this nutrient, or {@code null}
     * if the default renderer should be used.
     *
     * @return the custom {@link NutrientRenderer}, or {@code null}
     */
    @Nullable
    public NutrientRenderer getCustomRenderer() {
        return customRenderer;
    }

    /**
     * Returns whether this nutrient is beneficial (high values are good).
     * When false, high values are treated as bad (e.g. sugars).
     *
     * @return {@code true} if high values are good, {@code false} if high values are bad
     */
    public boolean isBeneficial() {
        return beneficial;
    }

    /**
     * Builder for constructing {@link NutrientDefinition} instances.
     */
    public static final class Builder {

        private final String id;
        private String displayName = "";
        private int color = 0xFFFFFFFF;
        private float defaultDecayRate = 0.001f;
        private float criticalThreshold = 0.1f;
        private float lowThreshold = 0.3f;
        private float excessThreshold = 0.9f;
        @Nullable
        private NutrientRenderer customRenderer;
        private boolean beneficial = true;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the human-readable display name for this nutrient.
         *
         * @param displayName the display name (e.g. "Vitamin C")
         * @return this builder for chaining
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the ARGB color used for HUD rendering.
         *
         * @param color the packed ARGB color integer
         * @return this builder for chaining
         */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /**
         * Sets the default decay rate per tick for this nutrient.
         *
         * @param rate the decay rate (must be non-negative)
         * @return this builder for chaining
         */
        public Builder defaultDecayRate(float rate) {
            this.defaultDecayRate = rate;
            return this;
        }

        /**
         * Sets the threshold below which the nutrient is considered critically low.
         *
         * @param threshold the critical threshold (0.0 to 1.0)
         * @return this builder for chaining
         */
        public Builder criticalThreshold(float threshold) {
            this.criticalThreshold = threshold;
            return this;
        }

        /**
         * Sets the threshold below which the nutrient is considered low.
         *
         * @param threshold the low threshold (0.0 to 1.0)
         * @return this builder for chaining
         */
        public Builder lowThreshold(float threshold) {
            this.lowThreshold = threshold;
            return this;
        }

        /**
         * Sets the threshold above which the nutrient is considered excessive.
         *
         * @param threshold the excess threshold (0.0 to 1.0)
         * @return this builder for chaining
         */
        public Builder excessThreshold(float threshold) {
            this.excessThreshold = threshold;
            return this;
        }

        /**
         * Sets an optional custom renderer to replace the default HUD visualization.
         *
         * @param renderer the custom {@link NutrientRenderer} implementation, or {@code null} for default
         * @return this builder for chaining
         */
        public Builder customRenderer(@Nullable NutrientRenderer renderer) {
            this.customRenderer = renderer;
            return this;
        }

        /**
         * Sets whether this nutrient is beneficial (high values are good).
         * When false, high values are treated as bad (e.g. sugars).
         * Default: true.
         *
         * @param beneficial {@code true} if high values are good, {@code false} if high values are bad
         * @return this builder for chaining
         */
        public Builder beneficial(boolean beneficial) {
            this.beneficial = beneficial;
            return this;
        }

        /**
         * Builds and returns the immutable {@link NutrientDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        public NutrientDefinition build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (displayName == null) {
                throw new IllegalStateException("displayName is required");
            }
            return new NutrientDefinition(
                    id,
                    displayName,
                    color,
                    defaultDecayRate,
                    criticalThreshold,
                    lowThreshold,
                    excessThreshold,
                    customRenderer,
                    beneficial
            );
        }
    }
}
