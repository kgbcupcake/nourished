package dev.maire.nourished.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired before a nutrient gain is applied to a player, allowing any mod to
 * intercept and multiply, reduce, or cancel the gain dynamically.
 *
 * <p>This event is cancellable. Cancelling it prevents the nutrient gain
 * from being applied entirely. Modifying {@link #setAmount(float)} changes
 * the final nutrient gain value.</p>
 *
 * <p>Subscribe to this event on the NeoForge event bus to implement dynamic
 * nutrient gain modifiers (e.g. potion effects, equipment bonuses, debuffs).</p>
 */
@ApiStatus.Stable
public class NutrientModifierEvent extends Event implements ICancellableEvent {

    private final Player player;
    private final ResourceLocation foodId;
    private final String nutrientKey;
    private float amount;
    private boolean canceled;

    /**
     * Constructs a new nutrient modifier event.
     *
     * @param player      the player about to receive the nutrient gain
     * @param foodId      the registry identifier of the food providing the nutrient
     * @param nutrientKey the key of the nutrient being gained
     * @param amount      the original nutrient gain amount before modification
     */
    public NutrientModifierEvent(Player player, ResourceLocation foodId, String nutrientKey, float amount) {
        this.player = player;
        this.foodId = foodId;
        this.nutrientKey = nutrientKey;
        this.amount = amount;
    }

    /**
     * Returns the player about to receive the nutrient gain.
     *
     * @return the target player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the registry identifier of the food providing the nutrient.
     *
     * @return the food's {@link ResourceLocation}
     */
    public ResourceLocation getFoodId() {
        return foodId;
    }

    /**
     * Returns the key of the nutrient being gained.
     *
     * @return the nutrient identifier string
     */
    public String getNutrientKey() {
        return nutrientKey;
    }

    /**
     * Returns the current nutrient gain amount (may have been modified by
     * earlier event handlers).
     *
     * @return the current gain amount
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Sets the nutrient gain amount. Use this to multiply, reduce, or zero out
     * the gain. Setting to zero is equivalent to cancelling but allows subsequent
     * handlers to still observe the event.
     *
     * @param amount the new nutrient gain amount (may be zero or negative)
     */
    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }
}
