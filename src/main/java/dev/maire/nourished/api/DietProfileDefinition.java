package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Defines a named diet archetype (e.g. Carnivore, Vegan, Mediterranean) with
 * custom thresholds, decay rates, and bonus effects per archetype.
 *
 * <p>Mods or modpacks can register profiles that players can switch between,
 * altering the nutrient system's behaviour to match a dietary philosophy.</p>
 *
 * <p>Use the {@link Builder} to construct instances and register them via
 * {@link NourishedAPI#registerDietProfile(DietProfileDefinition)}.</p>
 */
@ApiStatus.Experimental
public final class DietProfileDefinition {

    private final String id;
    private final String displayName;
    private final Map<String, Float> customThresholds;
    private final Map<String, Float> customDecayRates;
    private final List<ResourceLocation> bonusEffects;
    @Nullable
    private final String description;

    private DietProfileDefinition(
            String id,
            String displayName,
            Map<String, Float> customThresholds,
            Map<String, Float> customDecayRates,
            List<ResourceLocation> bonusEffects,
            @Nullable String description
    ) {
        this.id = id;
        this.displayName = displayName;
        this.customThresholds = Collections.unmodifiableMap(customThresholds);
        this.customDecayRates = Collections.unmodifiableMap(customDecayRates);
        this.bonusEffects = Collections.unmodifiableList(bonusEffects);
        this.description = description;
    }

    /**
     * Creates a new builder for a diet profile with the given identifier.
     *
     * @param id the unique internal key for this profile (e.g. "carnivore")
     * @return a new {@link Builder} instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique internal identifier for this diet profile.
     *
     * @return the profile key string
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the human-readable display name for this diet profile.
     *
     * @return the display name (e.g. "Mediterranean Diet")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns custom nutrient thresholds overriding the defaults when this profile
     * is active. Keys are nutrient identifiers, values are threshold overrides.
     *
     * @return an unmodifiable map of nutrient key to threshold value
     */
    public Map<String, Float> getCustomThresholds() {
        return customThresholds;
    }

    /**
     * Returns custom decay rate overrides for nutrients when this profile is active.
     *
     * @return an unmodifiable map of nutrient key to decay rate override
     */
    public Map<String, Float> getCustomDecayRates() {
        return customDecayRates;
    }

    /**
     * Returns the list of bonus effects granted while this diet profile is active
     * and the player is meeting the profile's requirements.
     *
     * @return an unmodifiable list of effect {@link ResourceLocation} identifiers
     */
    public List<ResourceLocation> getBonusEffects() {
        return bonusEffects;
    }

    /**
     * Returns an optional description of this diet profile for display in UIs.
     *
     * @return the profile description, or {@code null} if not set
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Builder for constructing {@link DietProfileDefinition} instances.
     */
    public static final class Builder {

        private final String id;
        private String displayName = "";
        private final Map<String, Float> customThresholds = new java.util.HashMap<>();
        private final Map<String, Float> customDecayRates = new java.util.HashMap<>();
        private final List<ResourceLocation> bonusEffects = new java.util.ArrayList<>();
        @Nullable
        private String description;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the human-readable display name for this profile.
         *
         * @param displayName the display name
         * @return this builder for chaining
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets a custom threshold override for a specific nutrient under this profile.
         *
         * @param nutrientKey the nutrient identifier
         * @param threshold   the threshold value (0.0 to 1.0)
         * @return this builder for chaining
         */
        public Builder customThreshold(String nutrientKey, float threshold) {
            this.customThresholds.put(nutrientKey, threshold);
            return this;
        }

        /**
         * Sets a custom decay rate override for a specific nutrient under this profile.
         *
         * @param nutrientKey the nutrient identifier
         * @param decayRate   the decay rate per tick
         * @return this builder for chaining
         */
        public Builder customDecayRate(String nutrientKey, float decayRate) {
            this.customDecayRates.put(nutrientKey, decayRate);
            return this;
        }

        /**
         * Adds a bonus effect granted when this profile is active and satisfied.
         *
         * @param effectId the effect's {@link ResourceLocation}
         * @return this builder for chaining
         */
        public Builder addBonusEffect(ResourceLocation effectId) {
            this.bonusEffects.add(effectId);
            return this;
        }

        /**
         * Sets an optional description for this diet profile.
         *
         * @param description a brief text description
         * @return this builder for chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns the immutable {@link DietProfileDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        public DietProfileDefinition build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (displayName == null) {
                throw new IllegalStateException("displayName is required");
            }
            if (customThresholds == null) {
                throw new IllegalStateException("customThresholds is required");
            }
            if (customDecayRates == null) {
                throw new IllegalStateException("customDecayRates is required");
            }
            if (bonusEffects == null) {
                throw new IllegalStateException("bonusEffects is required");
            }
            return new DietProfileDefinition(
                    id,
                    displayName,
                    customThresholds,
                    customDecayRates,
                    bonusEffects,
                    description
            );
        }
    }
}
