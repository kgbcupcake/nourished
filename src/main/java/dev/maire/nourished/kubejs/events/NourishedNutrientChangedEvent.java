package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MarieEvents;
import net.minecraft.world.entity.player.Player;

@ApiStatus.Experimental
public class NourishedNutrientChangedEvent implements KubeEvent {

    public final Player player;
    public final String playerId;
    public final String nutrientKey;
    public final float oldValue;
    public final float newValue;

    public NourishedNutrientChangedEvent() {
        this.player = null;
        this.playerId = "";
        this.nutrientKey = "";
        this.oldValue = 0f;
        this.newValue = 0f;
    }

    public NourishedNutrientChangedEvent(MarieEvents.ValueChangedEvent event) {
        this.player = event.getPlayer();
        this.playerId = event.getPlayer().getUUID().toString();
        this.nutrientKey = event.getValueKey();
        this.oldValue = event.getOldValue();
        this.newValue = event.getNewValue();
    }
}
