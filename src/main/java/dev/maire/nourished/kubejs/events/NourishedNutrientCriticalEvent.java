package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MarieEvents;
import net.minecraft.world.entity.player.Player;

@ApiStatus.Experimental
public class NourishedNutrientCriticalEvent implements KubeEvent {

    public final Player player;
    public final String playerId;
    public final String nutrientKey;

    public NourishedNutrientCriticalEvent() {
        this.player = null;
        this.playerId = "";
        this.nutrientKey = "";
    }

    public NourishedNutrientCriticalEvent(MarieEvents.ValueCriticalEvent event) {
        this.player = event.getPlayer();
        this.playerId = event.getPlayer().getUUID().toString();
        this.nutrientKey = event.getValueKey();
    }
}
