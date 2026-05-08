package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Defines a status effect that should be applied when a nutrient crosses
 * a specified threshold boundary.
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link NourishedAPI#registerCustomEffect(EffectDefinition)}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * EffectDefinition ironDeficiency = EffectDefinition.builder()
 *     .nutrientKey("iron")
 *     .threshold(0.15f)
 *     .thresholdType(ThresholdType.LOW)
 *     .effectId(ResourceLocation.parse("minecraft:slowness"))
 *     .amplifier(1)
 *     .duration(200)
 *     .build();
 * }</pre>
 */
@ApiStatus.Stable
public final class EffectDefinition {

    /**
     * Classifies the type of threshold crossing that triggers an effect.
     */
    public enum ThresholdType {
        /** Nutrient has dropped below the critical threshold. */
        CRITICAL,
        /** Nutrient has dropped below the low threshold. */
        LOW,
        /** Nutrient has exceeded the excess threshold. */
        EXCESS,
        /** Nutrient is within a bonus range (all thresholds satisfied optimally). */
        BONUS
    }

    private final String nutrientKey;
    private final float threshold;
    private final ThresholdType thresholdType;
    private final ResourceLocation effectId;
    private final int amplifier;
    private final int duration;

    private EffectDefinition(
            String nutrientKey,
            float threshold,
            ThresholdType thresholdType,
            ResourceLocation effectId,
            int amplifier,
            int duration
    ) {
        this.nutrientKey = nutrientKey;
        this.threshold = threshold;
        this.thresholdType = thresholdType;
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.duration = duration;
    }

    /**
     * Creates a new builder for an effect definition.
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the nutrient key this effect is bound to.
     *
     * @return the nutrient identifier string (e.g. "iron")
     */
    public String getNutrientKey() {
        return nutrientKey;
    }

    /**
     * Returns the numeric threshold value at which this effect activates.
     *
     * @return the threshold as a normalized float (0.0 to 1.0)
     */
    public float getThreshold() {
        return threshold;
    }

    /**
     * Returns the type of threshold crossing that triggers this effect.
     *
     * @return the {@link ThresholdType} enum value
     */
    public ThresholdType getThresholdType() {
        return thresholdType;
    }

    /**
     * Returns the registry identifier of the mob effect to apply.
     *
     * @return the effect's {@link ResourceLocation}
     */
    public ResourceLocation getEffectId() {
        return effectId;
    }

    /**
     * Returns the amplifier level for the applied effect (0-indexed).
     *
     * @return the effect amplifier
     */
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Returns the duration in ticks for the applied effect.
     *
     * @return the effect duration in game ticks
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Builder for constructing {@link EffectDefinition} instances.
     */
    public static final class Builder {

        private String nutrientKey;
        private float threshold;
        private ThresholdType thresholdType;
        private ResourceLocation effectId;
        private int amplifier;
        private int duration;

        private Builder() {}

        /**
         * Sets the nutrient key this effect is bound to.
         *
         * @param nutrientKey the nutrient identifier (e.g. "protein")
         * @return this builder for chaining
         */
        public Builder nutrientKey(String nutrientKey) {
            this.nutrientKey = nutrientKey;
            return this;
        }

        /**
         * Sets the numeric threshold that triggers this effect.
         *
         * @param threshold the activation threshold (0.0 to 1.0)
         * @return this builder for chaining
         */
        public Builder threshold(float threshold) {
            this.threshold = threshold;
            return this;
        }

        /**
         * Sets the type of threshold crossing that activates this effect.
         *
         * @param type the {@link ThresholdType} enum value
         * @return this builder for chaining
         */
        public Builder thresholdType(ThresholdType type) {
            this.thresholdType = type;
            return this;
        }

        /**
         * Sets the registry identifier of the mob effect to apply.
         *
         * @param effectId the effect's {@link ResourceLocation}
         * @return this builder for chaining
         */
        public Builder effectId(ResourceLocation effectId) {
            this.effectId = effectId;
            return this;
        }

        /**
         * Sets the amplifier level for the applied effect.
         *
         * @param amplifier the effect amplifier (0-indexed)
         * @return this builder for chaining
         */
        public Builder amplifier(int amplifier) {
            this.amplifier = amplifier;
            return this;
        }

        /**
         * Sets the duration for the applied effect in game ticks.
         *
         * @param duration the effect duration in ticks
         * @return this builder for chaining
         */
        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Builds and returns the immutable {@link EffectDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        public EffectDefinition build() {
            if (nutrientKey == null) {
                throw new IllegalStateException("nutrientKey is required");
            }
            if (thresholdType == null) {
                throw new IllegalStateException("thresholdType is required");
            }
            if (effectId == null) {
                throw new IllegalStateException("effectId is required");
            }
            return new EffectDefinition(nutrientKey, threshold, thresholdType, effectId, amplifier, duration);
        }
    }
}
