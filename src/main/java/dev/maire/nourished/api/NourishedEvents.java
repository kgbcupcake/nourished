package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * Contains all NeoForge event classes fired by the Nourished nutrition system.
 *
 * <p>Subscribe to these events via {@code @SubscribeEvent} on the NeoForge event bus
 * to react to nutrition changes, critical states, and food consumption.</p>
 */
public final class NourishedEvents {

    private NourishedEvents() {}

    /**
     * Fired when a player's nutrient level changes value.
     *
     * <p>This event is fired after the nutrient value has been updated. It is
     * not cancellable. Use {@link NutrientModifierEvent} to intercept changes
     * before they are applied.</p>
     */
    @ApiStatus.Stable
    public static class NutrientChangedEvent extends Event {

        private final Player player;
        private final String nutrientKey;
        private final float oldValue;
        private final float newValue;

        /**
         * Constructs a new nutrient changed event.
         *
         * @param player      the player whose nutrient level changed
         * @param nutrientKey the key of the nutrient that changed
         * @param oldValue    the previous nutrient level
         * @param newValue    the new nutrient level
         */
        public NutrientChangedEvent(Player player, String nutrientKey, float oldValue, float newValue) {
            this.player = player;
            this.nutrientKey = nutrientKey;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        /**
         * Returns the player whose nutrient level changed.
         *
         * @return the affected player
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the key of the nutrient that changed.
         *
         * @return the nutrient identifier string
         */
        public String getNutrientKey() {
            return nutrientKey;
        }

        /**
         * Returns the previous nutrient level before the change.
         *
         * @return the old value as a normalized float
         */
        public float getOldValue() {
            return oldValue;
        }

        /**
         * Returns the new nutrient level after the change.
         *
         * @return the new value as a normalized float
         */
        public float getNewValue() {
            return newValue;
        }
    }

    /**
     * Fired when a player's nutrient level drops below the critical threshold.
     *
     * <p>This is an informational event fired once per threshold crossing
     * (not every tick while critical). Not cancellable.</p>
     */
    @ApiStatus.Stable
    public static class NutrientCriticalEvent extends Event {

        private final Player player;
        private final String nutrientKey;

        /**
         * Constructs a new nutrient critical event.
         *
         * @param player      the player whose nutrient became critical
         * @param nutrientKey the key of the nutrient that crossed the critical threshold
         */
        public NutrientCriticalEvent(Player player, String nutrientKey) {
            this.player = player;
            this.nutrientKey = nutrientKey;
        }

        /**
         * Returns the player whose nutrient reached critical level.
         *
         * @return the affected player
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the key of the nutrient that is critically low.
         *
         * @return the nutrient identifier string
         */
        public String getNutrientKey() {
            return nutrientKey;
        }
    }

    /**
     * Fired when a player's nutrient level exceeds the excess threshold.
     *
     * <p>This is an informational event fired once per threshold crossing
     * (not every tick while excessive). Not cancellable.</p>
     */
    @ApiStatus.Stable
    public static class NutrientExcessEvent extends Event {

        private final Player player;
        private final String nutrientKey;

        /**
         * Constructs a new nutrient excess event.
         *
         * @param player      the player whose nutrient became excessive
         * @param nutrientKey the key of the nutrient that crossed the excess threshold
         */
        public NutrientExcessEvent(Player player, String nutrientKey) {
            this.player = player;
            this.nutrientKey = nutrientKey;
        }

        /**
         * Returns the player whose nutrient reached excess level.
         *
         * @return the affected player
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the key of the nutrient that is excessively high.
         *
         * @return the nutrient identifier string
         */
        public String getNutrientKey() {
            return nutrientKey;
        }
    }

    /**
     * Fired when a player eats a food item and gains nutrition from it.
     *
     * <p>This event is fired after nutrient gains are calculated but the
     * informational snapshot is taken at the point of consumption. Not cancellable.</p>
     */
    @ApiStatus.Stable
    public static class FoodEatenEvent extends Event {

        private final Player player;
        private final ResourceLocation foodId;
        private final String nutrientKey;
        private final float amount;

        /**
         * Constructs a new food eaten event.
         *
         * @param player      the player who consumed the food
         * @param foodId      the registry identifier of the consumed food item
         * @param nutrientKey the primary nutrient gained from this food
         * @param amount      the amount of nutrient gained
         */
        public FoodEatenEvent(Player player, ResourceLocation foodId, String nutrientKey, float amount) {
            this.player = player;
            this.foodId = foodId;
            this.nutrientKey = nutrientKey;
            this.amount = amount;
        }

        /**
         * Returns the player who consumed the food.
         *
         * @return the player
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the registry identifier of the consumed food item.
         *
         * @return the food's {@link ResourceLocation}
         */
        public ResourceLocation getFoodId() {
            return foodId;
        }

        /**
         * Returns the primary nutrient key gained from this food.
         *
         * @return the nutrient identifier string
         */
        public String getNutrientKey() {
            return nutrientKey;
        }

        /**
         * Returns the amount of nutrient gained from this food consumption.
         *
         * @return the nutrient gain amount
         */
        public float getAmount() {
            return amount;
        }
    }
}
