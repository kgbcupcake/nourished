package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Defines a milestone that fires once when a player reaches a cumulative
 * nutrition goal. Functions as an achievement system built into the API.
 *
 * <p>Milestones are persistent across sessions and fire exactly once per player.
 * They can grant effects, trigger advancements, or invoke custom callbacks.</p>
 *
 * <p>Use the {@link Builder} to construct instances and register them
 * via {@link NourishedAPI#registerMilestone(NutrientMilestoneDefinition)}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * NutrientMilestoneDefinition ironMaster = NutrientMilestoneDefinition.builder("iron_master")
 *     .nutrientKey("iron")
 *     .cumulativeGoal(500.0f)
 *     .rewardEffect(ResourceLocation.parse("minecraft:resistance"))
 *     .rewardAmplifier(0)
 *     .rewardDuration(6000)
 *     .build();
 * }</pre>
 */
@ApiStatus.Experimental
public final class NutrientMilestoneDefinition {

    private final String id;
    private final String nutrientKey;
    private final float cumulativeGoal;
    @Nullable
    private final ResourceLocation rewardEffectId;
    private final int rewardAmplifier;
    private final int rewardDuration;
    @Nullable
    private final ResourceLocation advancementId;

    private NutrientMilestoneDefinition(
            String id,
            String nutrientKey,
            float cumulativeGoal,
            @Nullable ResourceLocation rewardEffectId,
            int rewardAmplifier,
            int rewardDuration,
            @Nullable ResourceLocation advancementId
    ) {
        this.id = id;
        this.nutrientKey = nutrientKey;
        this.cumulativeGoal = cumulativeGoal;
        this.rewardEffectId = rewardEffectId;
        this.rewardAmplifier = rewardAmplifier;
        this.rewardDuration = rewardDuration;
        this.advancementId = advancementId;
    }

    /**
     * Creates a new builder for a nutrient milestone.
     *
     * @param id the unique identifier for this milestone (e.g. "iron_master")
     * @return a new {@link Builder} instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Returns the unique identifier of this milestone.
     *
     * @return the milestone id string
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the nutrient key this milestone tracks.
     *
     * @return the nutrient identifier string
     */
    public String getNutrientKey() {
        return nutrientKey;
    }

    /**
     * Returns the cumulative nutrient amount required to trigger this milestone.
     *
     * @return the cumulative goal value
     */
    public float getCumulativeGoal() {
        return cumulativeGoal;
    }

    /**
     * Returns the mob effect granted as a reward when the milestone triggers,
     * or {@code null} if no effect reward is configured.
     *
     * @return the reward effect's {@link ResourceLocation}, or {@code null}
     */
    @Nullable
    public ResourceLocation getRewardEffectId() {
        return rewardEffectId;
    }

    /**
     * Returns the amplifier for the reward effect.
     *
     * @return the effect amplifier (0-indexed)
     */
    public int getRewardAmplifier() {
        return rewardAmplifier;
    }

    /**
     * Returns the duration of the reward effect in game ticks.
     *
     * @return the reward duration in ticks
     */
    public int getRewardDuration() {
        return rewardDuration;
    }

    /**
     * Returns the advancement to grant when this milestone triggers,
     * or {@code null} if no advancement is configured.
     *
     * @return the advancement's {@link ResourceLocation}, or {@code null}
     */
    @Nullable
    public ResourceLocation getAdvancementId() {
        return advancementId;
    }

    /**
     * Builder for constructing {@link NutrientMilestoneDefinition} instances.
     */
    public static final class Builder {

        private final String id;
        private String nutrientKey;
        private float cumulativeGoal;
        @Nullable
        private ResourceLocation rewardEffectId;
        private int rewardAmplifier = 0;
        private int rewardDuration = 200;
        @Nullable
        private ResourceLocation advancementId;

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the nutrient key this milestone tracks.
         *
         * @param nutrientKey the nutrient identifier (e.g. "iron")
         * @return this builder for chaining
         */
        public Builder nutrientKey(String nutrientKey) {
            this.nutrientKey = nutrientKey;
            return this;
        }

        /**
         * Sets the cumulative nutrient amount required to trigger this milestone.
         *
         * @param goal the cumulative goal value
         * @return this builder for chaining
         */
        public Builder cumulativeGoal(float goal) {
            this.cumulativeGoal = goal;
            return this;
        }

        /**
         * Sets the mob effect granted as a one-time reward.
         *
         * @param effectId the reward effect's {@link ResourceLocation}
         * @return this builder for chaining
         */
        public Builder rewardEffect(ResourceLocation effectId) {
            this.rewardEffectId = effectId;
            return this;
        }

        /**
         * Sets the amplifier for the reward effect.
         *
         * @param amplifier the effect amplifier (0-indexed)
         * @return this builder for chaining
         */
        public Builder rewardAmplifier(int amplifier) {
            this.rewardAmplifier = amplifier;
            return this;
        }

        /**
         * Sets the duration for the reward effect.
         *
         * @param duration the duration in game ticks
         * @return this builder for chaining
         */
        public Builder rewardDuration(int duration) {
            this.rewardDuration = duration;
            return this;
        }

        /**
         * Sets an advancement to grant when the milestone triggers.
         *
         * @param advancementId the advancement's {@link ResourceLocation}
         * @return this builder for chaining
         */
        public Builder advancement(ResourceLocation advancementId) {
            this.advancementId = advancementId;
            return this;
        }

        /**
         * Builds and returns the immutable {@link NutrientMilestoneDefinition}.
         *
         * @return the constructed definition
         * @throws IllegalStateException if required fields are missing or invalid
         */
        public NutrientMilestoneDefinition build() {
            if (id == null) {
                throw new IllegalStateException("id is required");
            }
            if (nutrientKey == null) {
                throw new IllegalStateException("nutrientKey is required");
            }
            return new NutrientMilestoneDefinition(
                    id,
                    nutrientKey,
                    cumulativeGoal,
                    rewardEffectId,
                    rewardAmplifier,
                    rewardDuration,
                    advancementId
            );
        }
    }
}
