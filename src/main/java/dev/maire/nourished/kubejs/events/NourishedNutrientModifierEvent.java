package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import net.minecraft.world.entity.player.Player;

@ApiStatus.Experimental
public class NourishedNutrientModifierEvent implements KubeEvent {

    public final Player player;
    public final String playerId;
    public final String itemId;
    public final String nutrientKey;
    public float amount;

    public NourishedNutrientModifierEvent() {
        this.player = null;
        this.playerId = "";
        this.itemId = "";
        this.nutrientKey = "";
        this.amount = 0f;
    }

    public NourishedNutrientModifierEvent(Player player, String itemId, String nutrientKey, float amount) {
        this.player = player;
        this.playerId = player.getUUID().toString();
        this.itemId = itemId;
        this.nutrientKey = nutrientKey;
        this.amount = amount;
    }

    public void cancel() {
        this.amount = 0f;
    }
}
