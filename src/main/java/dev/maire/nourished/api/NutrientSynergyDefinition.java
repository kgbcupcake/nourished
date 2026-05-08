package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Defines a synergy interaction between two nutrients. When both nutrients
 * satisfy their respective conditions simultaneously, a bonus or penalty
 * effect is applied to the player.
 *
 * <p>This is a novel mechanic — no other nutrition mod implements nutrient
 * combo interactions. Examples:</p>
 * <ul>
 *   <li>High Protein + High Carbs = Strength bonus</li>
 *   <li>Low Protein + High Fats = Slowness penalty</li>
 * </ul>
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link NourishedAPI#registerNutrientSynergy(NutrientSynergyDefinition)}.</p>
 */
@ApiStatus.Experimental
public final class NutrientSynergyDefinition {

    /**
     * Defines whether the nutrient condition requires a high or low level.
     */
    public enum LevelCondition {
        /** The nutrient must be above its excess threshold. */
        HIGH,
        /** The nutrient must be below its low threshold. */
        LOW,
        /** The nutrient must be in its optimal range (between low and excess). */
        OPTIMAL
    }

    private final String id;
    private final String nutrientKeyA;
    private final LevelCondition conditionA;
    private final String nutrientKeyB;
    private final LevelCondition conditionB;
    @Nullable
    private final ResourceLocation bonusEffectId;
    private final int effectAmplifier;
    private final int effectDuration;
    private final boolean isPenalty;

    private NutrientSynergyDefinition(
            String id,
            String nutrientKeyA,
            LevelCondition conditionA,
            String nutrientKeyB,
            LevelCondition conditionB,
            @Nullable ResourceLocation bonusEffectId,
            int effectAmplifier,
            int effectDuration,
            boolean isPenalty
    ) {
        this.id = id;
        this.nutrientKeyA = nutrientKeyA;
        this.conditionA = conditionA;
        this.nutrientKeyB = nutrientKeyB;
        this.conditionB = conditionB;
        this.bonusEffectId = bonusEffectId;
        this.effectAmplifier = effectAmplifier;
        this.effectDuration = effectDuration;
        this.isPenalty = isPenalty;
    }

    /**
     * Creates a new builder for a nutrient synergy definition.
     *
     * @param id the unique identifier for this synergy (e.g. "protein_carbs_strength")
     * @return a new {@link Builder} instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique identifier of this synergy definition.
     *
     * @return the synergy id string
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the key of the first nutrient in this synergy pair.
     *
     * @return the first nutrient key
     */
    public String getNutrientKeyA() {
        return nutrientKeyA;
    }

    /**
     * Returns the level condition required for the first nutrient.
     *
     * @return the {@link LevelCondition} for nutrient A
     */
    public LevelCondition getConditionA() {
        return conditionA;
    }

    /**
     * Returns the key of the second nutrient in this synergy pair.
     *
     * @return the second nutrient key
     */
    public String getNutrientKeyB() {
        return nutrientKeyB;
    }

    /**
     * Returns the level condition required for the second nutrient.
     *
     * @return the {@link LevelCondition} for nutrient B
     */
    public LevelCondition getConditionB() {
        return conditionB;
    }

    /**
     * Returns the mob effect to apply when the synergy conditions are met,
     * or {@code null} if this synergy uses a custom reward mechanism.
     *
     * @return the effect's {@link ResourceLocation}, or {@code null}
     */
    @Nullable
    public ResourceLocation getBonusEffectId() {
        return bonusEffectId;
    }

    /**
     * Returns the amplifier level of the synergy effect.
     *
     * @return the effect amplifier (0-indexed)
     */
    public int getEffectAmplifier() {
        return effectAmplifier;
    }

    /**
     * Returns the duration of the synergy effect in game ticks.
     *
     * @return the effect duration in ticks
     */
    public int getEffectDuration() {
        return effectDuration;
    }

    /**
     * Returns whether this synergy represents a penalty rather than a bonus.
     *
     * @return {@code true} if this is a penalty synergy
     */
    public boolean isPenalty() {
        return isPenalty;
    }

    /**
     * Builder for constructing {@link NutrientSynergyDefinition} instances.
     */
    public static final class Builder {

        private final String id;
        private String nutrientKeyA;
        private LevelCondition conditionA = LevelCondition.HIGH;
        private String nutrientKeyB;
        private LevelCondition conditionB = LevelCondition.HIGH;
        @Nullable
        private ResourceLocation bonusEffectId;
        private int effectAmplifier = 0;
        private int effectDuration = 200;
        private boolean isPenalty = false;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the first nutrient and its required condition.
         *
         * @param nutrientKey the first nutrient key (e.g. "protein")
         * @param condition   the level condition required
         * @return this builder for chaining
         */
        public Builder nutrientA(String nutrientKey, LevelCondition condition) {
            this.nutrientKeyA = nutrientKey;
            this.conditionA = condition;
            return this;
        }

        /**
         * Sets the second nutrient and its required condition.
         *
         * @param nutrientKey the second nutrient key (e.g. "carbs")
         * @param condition   the level condition required
         * @return this builder for chaining
         */
        public Builder nutrientB(String nutrientKey, LevelCondition condition) {
            this.nutrientKeyB = nutrientKey;
            this.conditionB = condition;
            return this;
        }

        /**
         * Sets the mob effect granted when both synergy conditions are satisfied.
         *
         * @param effectId the effect's {@link ResourceLocation}
         * @return this builder for chaining
         */
        public Builder bonusEffect(ResourceLocation effectId) {
            this.bonusEffectId = effectId;
            return this;
        }

        /**
         * Sets the amplifier for the synergy effect.
         *
         * @param amplifier the effect amplifier (0-indexed)
         * @return this builder for chaining
         */
        public Builder effectAmplifier(int amplifier) {
            this.effectAmplifier = amplifier;
            return this;
        }

        /**
         * Sets the duration of the synergy effect.
         *
         * @param duration the effect duration in game ticks
         * @return this builder for chaining
         */
        public Builder effectDuration(int duration) {
            this.effectDuration = duration;
            return this;
        }

        /**
         * Marks this synergy as a penalty (negative effect) rather than a bonus.
         *
         * @param penalty {@code true} if this synergy applies a penalty
         * @return this builder for chaining
         */
        public Builder penalty(boolean penalty) {
            this.isPenalty = penalty;
            return this;
        }

        /**
         * Builds and returns the immutable {@link NutrientSynergyDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        public NutrientSynergyDefinition build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (nutrientKeyA == null) {
                throw new IllegalStateException("nutrientKeyA is required");
            }
            if (conditionA == null) {
                throw new IllegalStateException("conditionA is required");
            }
            if (nutrientKeyB == null) {
                throw new IllegalStateException("nutrientKeyB is required");
            }
            if (conditionB == null) {
                throw new IllegalStateException("conditionB is required");
            }
            return new NutrientSynergyDefinition(
                    id,
                    nutrientKeyA,
                    conditionA,
                    nutrientKeyB,
                    conditionB,
                    bonusEffectId,
                    effectAmplifier,
                    effectDuration,
                    isPenalty
            );
        }
    }
}
