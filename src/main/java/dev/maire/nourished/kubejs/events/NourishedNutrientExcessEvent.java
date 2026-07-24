package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marie.MarieEvents;
import net.minecraft.world.entity.player.Player;

@ApiStatus.Experimental
public class NourishedNutrientExcessEvent implements KubeEvent {

    public final Player player;
    public final String playerId;
    public final String nutrientKey;

    public NourishedNutrientExcessEvent() {
        this.player = null;
        this.playerId = "";
        this.nutrientKey = "";
    }

    public NourishedNutrientExcessEvent(MarieEvents.ValueExcessEvent event) {
        this.player = event.getPlayer();
        this.playerId = event.getPlayer().getUUID().toString();
        this.nutrientKey = event.getValueKey();
    }
}
