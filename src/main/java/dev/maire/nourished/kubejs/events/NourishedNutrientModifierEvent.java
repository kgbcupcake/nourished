package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;

@ApiStatus.Experimental
public class NourishedNutrientModifierEvent implements KubeEvent {

    public final String playerId;
    public final String itemId;
    public final String nutrientKey;
    public float amount;

    public NourishedNutrientModifierEvent() {
        this.playerId = "";
        this.itemId = "";
        this.nutrientKey = "";
        this.amount = 0f;
    }

    public NourishedNutrientModifierEvent(String playerId, String itemId, String nutrientKey, float amount) {
        this.playerId = playerId;
        this.itemId = itemId;
        this.nutrientKey = nutrientKey;
        this.amount = amount;
    }

    public void cancel() {
        this.amount = 0f;
    }
}
